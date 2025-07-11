package org.cloud.user.controller;

import org.cloud.user.dto.ApiResponse;
import org.cloud.user.dto.UserInfo;
import org.cloud.user.exception.UserNotFound;
import org.cloud.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
public class UserController {
    private final static Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final static String VERIFICATION_CODE_PREFIX = "verification:"; // 验证码的 redis 键前缀

    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2, // 核心线程数
            Runtime.getRuntime().availableProcessors() * 4, // 最大线程数
            60L,  // 线程空闲时间
            TimeUnit.SECONDS, // 空闲时间单位
            new LinkedBlockingQueue<>(1024), // 阻塞队列(可以指定队列大小，避免内存溢出)
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略(当队列满了，新任务会在调用线程中执行)
    );

    @Autowired
    public UserController(UserService userService, ReactiveRedisTemplate<String, String> redisTemplate) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/register")
    public Mono<ApiResponse<?>> register(@RequestBody Map<String, String> request) {
        // 获取请求参数
        String email = request.get("email");
        String username = request.get("username");
        String password = request.get("password");
        String code = request.get("code");

        if (email == null || username == null || password == null || code == null) {
            return Mono.just(ApiResponse.failure(400, "请求参数错误"));
        }

        // 检查验证码是否正确
        String key = VERIFICATION_CODE_PREFIX + email;

        return redisTemplate.opsForValue().get(key)
                .flatMap(verificationCode -> {
                    if (verificationCode == null) {
                        return Mono.just(ApiResponse.failure(401, "验证码已过期"));
                    }
                    if (!code.equals(verificationCode)) {
                        return Mono.just(ApiResponse.failure(401, "验证码错误"));
                    }

                    // 注册
                    return userService.register(email, username, password)
                        .flatMap(userId -> {
                            if (userId == null) {
                                return Mono.just(ApiResponse.failure(400, "注册失败"));
                            } else {
                                return Mono.just(ApiResponse.success(userId));
                            }
                        })
                        .switchIfEmpty(Mono.just(ApiResponse.failure(400, "注册失败")));
                })
                .switchIfEmpty(Mono.just(ApiResponse.failure(401, "验证码已过期")));
    }

    @PostMapping("/login")
    public Mono<ApiResponse<?>> login(@RequestBody Map<String, Object> request) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    try {
                        // 获取请求参数
                        String email, password, code;
                        try {
                            email = (String) request.get("email");
                            password = (String) request.get("password");
                            code = (String) request.get("code");
                        } catch (Exception e) {
                            return ApiResponse.failure(400, "请求参数错误");
                        }

                        // 检查验证码是否正确
                        String key = VERIFICATION_CODE_PREFIX + email;
                        String verificationCode = redisTemplate.opsForValue().get(key).block();
                        if(verificationCode == null) return ApiResponse.failure(401, "验证码已过期");
                        if(!code.equals(verificationCode)) return ApiResponse.failure(401, "验证码错误");

                        // 登录
                        String userId = null;
                        try {
                            userId = userService.login(email, password);
                        } catch(UserNotFound e) {
                            return ApiResponse.failure( 404, email + " 尚未注册，请先注册");
                        }
                        if (userId == null) return ApiResponse.failure(401, "账号或密码错误");

                        // 生成 token，并将 token 存入 redis
                        String token = UUID.randomUUID().toString();
                        redisTemplate.opsForValue().set(token, userId, Duration.ofHours(24)).subscribe();

                        return ApiResponse.success(token);
                    } catch (Exception e) {
                        logger.error("登录发生错误：", e);
                        return ApiResponse.failure(500, "内部服务器错误");
                    }
                },
                executorService
            )
        );
    }

    @GetMapping("/info")
    public Mono<ApiResponse<?>> getUserInfo(ServerWebExchange exchange) {
        return Mono.fromFuture(
                CompletableFuture.supplyAsync(
                        () -> {
                            String token = extractToken(exchange.getRequest());

                            if (token == null) {
                                return Mono.just(ApiResponse.failure(401, "无效的Token"));
                            }

                            // 根据 token 获取用户id，用户id即 bucket
                            return redisTemplate.opsForValue().get(token).flatMap(
                                    userId -> {
                                        if (userId == null) {
                                            return Mono.just(ApiResponse.failure(401, "Token已过期或失效"));
                                        }

                                        try {
                                            UserInfo userInfo = userService.getUserInfo(userId);
                                            return Mono.just(ApiResponse.success(userInfo));
                                        } catch (Exception e) {
                                            return Mono.just(ApiResponse.failure(500, "内部服务器错误"));
                                        }
                                    }
                            );
                        },
                        executorService
                )
        ).flatMap(monoApiResponse -> monoApiResponse); // 展平嵌套的 Mono
    }

    /**
     * webflux 架构下提取token
     * @param request 请求
     * @return token
     */
    private String extractToken(ServerHttpRequest request) {
        // 获取Authorization头
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 提取Bearer后面的token
            return authHeader.substring(7);
        }
        return null;
    }

    @PostMapping("/logout")
    public Mono<ApiResponse<?>> logout(ServerWebExchange exchange) {
        // 提取Authorization头中Bearer后面的token
        String token = extractToken(exchange.getRequest());

        // 如果token为空，直接返回失败响应
        if (token == null) {
            return Mono.just(ApiResponse.failure(401, "无效的Token"));
        }

        // Redis中删除token
        return redisTemplate.opsForValue().delete(token)
            .flatMap(deletedCount -> {
                if (Boolean.TRUE.equals(deletedCount)) {
                    return Mono.just(ApiResponse.success("注销登录成功"));
                } else {
                    return Mono.just(ApiResponse.failure(401, "Token不存在或已失效"));
                }
            });
    }

    @PostMapping("/reset_pwd")
    public Mono<ApiResponse<?>> resetPassword(@RequestBody Map<String, Object> request) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    String email, code, newPassword;

                    try {
                        email = (String) request.get("email");
                        code = (String) request.get("code");
                        newPassword = (String) request.get("newPassword");
                    } catch (Exception e) {
                        return ApiResponse.failure(400, "请求参数错误");
                    }

                    // 检查验证码是否正确
                    String key = VERIFICATION_CODE_PREFIX + email;
                    String verificationCode = redisTemplate.opsForValue().get(key).block();
                    if(verificationCode == null) return ApiResponse.failure(401, "验证码已过期");
                    if(!code.equals(verificationCode)) return ApiResponse.failure(401, "验证码错误");

                    userService.resetPassword(email, newPassword);

                    return ApiResponse.success("密码重置成功");
                },
                executorService
            )
        );
    }

    @PostMapping("/update_pwd")
    public Mono<ApiResponse<?>> updatePassword(@RequestBody Map<String, Object> request) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    String email, password, newPassword;

                    try {
                        email = (String) request.get("email");
                        password = (String) request.get("password");
                        newPassword = (String) request.get("newPassword");
                    } catch (Exception e) {
                        return ApiResponse.failure(400, "请求参数错误");
                    }

                    // 更新密码
                    try {
                        boolean isSuccess = userService.updatePassword(email, password, newPassword);
                        if(!isSuccess) return ApiResponse.failure(401, "密码错误");
                    } catch (Exception e) {
                        return ApiResponse.failure(500, "内部服务器错误");
                    }

                    return ApiResponse.success("密码更新成功");
                },
                executorService
            )
        );
    }

    @PostMapping("/update_info")
    public Mono<ApiResponse<?>> updateUserInfo(@RequestBody UserInfo userInfo) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    try {
                        userService.updateUserInfo(userInfo);
                    } catch (Exception e) {
                        return ApiResponse.failure(500, "内部服务器错误");
                    }

                    return ApiResponse.success("用户信息更新成功");
                },
                executorService
            )
        );
    }

    @PostMapping("/update_used_capacity")
    public Mono<ApiResponse<?>> updateUsedCapacity(@RequestBody Map<String, Object> request) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    String userId;
                    BigDecimal usedCapacity;
                    try {
                        userId = (String) request.get("userId");
                        usedCapacity = (BigDecimal) request.get("usedCapacity");
                    } catch (Exception e) {
                        return ApiResponse.failure(400, "请求参数错误");
                    }

                    try {
                        userService.updateUsedCapacity(userId, usedCapacity);
                        return ApiResponse.success("已使用容量更新成功");
                    } catch (Exception e) {
                        return ApiResponse.failure(500, "内部服务器错误");
                    }
                },
                executorService
            )
        );
    }

    @GetMapping("/used_capacity")
    public Mono<ApiResponse<?>> getUsedCapacity(@RequestParam("userId") String userId) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    try {
                        BigDecimal usedCapacity = userService.getUsedCapacity(userId);
                        return ApiResponse.success(usedCapacity);
                    } catch (Exception e) {
                        return ApiResponse.failure(500, "内部服务器错误");
                    }
                },
                executorService
            )
        );
    }

    @GetMapping("/status")
    public Mono<ApiResponse<?>> getUserStatus(@RequestParam("userId") String userId) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    try {
                        String userStatus = userService.selectUserStatus(userId);
                        if(userStatus == null) return ApiResponse.failure(404, "用户不存在");
                        return ApiResponse.success(userStatus);
                    } catch (Exception e) {
                        return ApiResponse.failure(500, "内部服务器错误");
                    }
                },
                executorService
            )
        );
    }

    @PostMapping("/freeze")
    public Mono<ApiResponse<?>> freezeUser(@RequestParam("userId") String userId) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    try {
                        userService.freezeUser(userId);
                        return ApiResponse.success("用户冻结成功");
                    } catch (Exception e) {
                        return ApiResponse.failure(500, "内部服务器错误");
                    }
                },
                executorService
            )
        );
    }
}
