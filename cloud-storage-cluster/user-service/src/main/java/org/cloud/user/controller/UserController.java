package org.cloud.user.controller;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.cloud.user.dto.*;
import org.cloud.user.exception.InvalidImageTypeException;
import org.cloud.user.exception.RemoteFileSystemException;
import org.cloud.user.exception.UserAlreadyExistException;
import org.cloud.user.exception.UserNotFoundException;
import org.cloud.user.mappers.UserMapper;
import org.cloud.user.service.UserService;
import org.cloud.user.util.file.FileHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

@RestController
public class UserController {
    private final static Logger logger = LoggerFactory.getLogger(UserController.class);
    private final static String VERIFICATION_CODE_PREFIX = "verification:"; // 验证码的 redis 键前缀
    private final static String AVATAR_BUCKET = "user-avatars";

    private final UserService userService;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ExecutorService executorService;
    private final FileHasher fileHasher;
    private final MinioClient minioClient;
    private final UserMapper userMapper;

    @Autowired
    public UserController(UserService userService,
                          ReactiveRedisTemplate<String, String> redisTemplate,
                          ExecutorService executorService,
                          FileHasher fileHasher, MinioClient minioClient, UserMapper userMapper) {
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.executorService = executorService;
        this.fileHasher = fileHasher;
        this.minioClient = minioClient;
        this.userMapper = userMapper;
    }

    /**
     * 用户注册
     * @param registerRequest 注册请求对象
     * @return 注册成功，返回用户ID，否则返回错误信息
     */
    @PostMapping("/register")
    public Mono<ApiResponse<?>> register(@RequestBody @Validated RegisterRequest registerRequest) {
        // 获取请求参数
        String email = registerRequest.getEmail();
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String code = registerRequest.getCode();

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
                        .switchIfEmpty(Mono.just(ApiResponse.failure(400, "注册失败")))
                        .onErrorResume(
                                UserAlreadyExistException.class,
                                e -> Mono.just(ApiResponse.failure(400, e.getMessage()))
                        ).onErrorResume(
                                RemoteFileSystemException.class,
                                e -> Mono.just(ApiResponse.failure(500, e.getMessage()))
                        );
                })
                .switchIfEmpty(Mono.just(ApiResponse.failure(401, "验证码已过期")));
    }

    /**
     * 登录账号
     * @apiNote 登录成功后会生成一个 UUID 令牌存储到 redis 中
     * @param loginRequest 登录请求对象
     * @return 注册成功，返回UUID令牌，否则返回错误信息
     */
    @PostMapping("/login")
    public Mono<ApiResponse<?>> login(@RequestBody @Validated LoginRequest loginRequest) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    try {
                        // 获取请求参数
                        String email = loginRequest.getEmail();
                        String password = loginRequest.getPassword();
                        String code = loginRequest.getCode();

                        // 检查验证码是否正确
                        String key = VERIFICATION_CODE_PREFIX + email;
                        String verificationCode = redisTemplate.opsForValue().get(key).block();
                        if(verificationCode == null) return ApiResponse.failure(401, "验证码已过期");
                        if(!code.equals(verificationCode)) return ApiResponse.failure(401, "验证码错误");

                        // 登录
                        String userId = null;
                        try {
                            userId = userService.login(email, password);
                        } catch(UserNotFoundException e) {
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

    /**
     * 查询用户信息
     * @apiNote 需要携带 Authorization 请求头，格式 Bearer {token}
     * @ignoreParam exchange
     * @param exchange ServerHttpRequest 和 ServerHttpResponse 的封装，用于从请求中获取 Authorization 的 token
     * @return UserInfo 对象
     */
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
                                        try {
                                            UserInfo userInfo = userService.getUserInfo(userId);
                                            return Mono.just(ApiResponse.success(userInfo));
                                        } catch (Exception e) {
                                            return Mono.just(ApiResponse.failure(500, "内部服务器错误"));
                                        }
                                    }
                            ).switchIfEmpty(Mono.just(ApiResponse.failure(401, "无效的Token")));
                        },
                        executorService
                )
        ).flatMap(monoApiResponse -> monoApiResponse); // 展平嵌套的 Mono
    }

    /**
     * webflux 架构下提取token
     * @param request http 请求
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

    /**
     * 退出登录
     * @apiNote 需要携带 Authorization 请求头，格式 Bearer {token}，退出登录成功后会删除 redis 中的 token
     * @ignoreParam exchange
     * @param exchange ServerHttpRequest 和 ServerHttpResponse 的封装，用于从请求中获取 Authorization 的 token
     * @return 操作结果
     */
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

    /**
     * 重置密码
     * @apiNote 用于忘记密码时通过邮箱验证重置密码
     * @param resetPwdRequest 重置密码请求对象
     * @return 操作结果
     */
    @PostMapping("/reset_pwd")
    public Mono<ApiResponse<?>> resetPassword(@RequestBody @Validated ResetPwdRequest resetPwdRequest) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    String email = resetPwdRequest.getEmail();
                    String code = resetPwdRequest.getCode();
                    String newPassword = resetPwdRequest.getNewPassword();

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

    /**
     * 修改密码
     * @apiNote 使用正确的账号旧密码来更改密码
     * @param updatePwdRequest 更新密码请求对象
     * @return 操作结果
     */
    @PostMapping("/update_pwd")
    public Mono<ApiResponse<?>> updatePassword(@RequestBody @Validated UpdatePwdRequest updatePwdRequest) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    String email = updatePwdRequest.getEmail();
                    String password = updatePwdRequest.getPassword();
                    String newPassword = updatePwdRequest.getNewPassword();

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

    /**
     * 更新用户信息
     * @param updateUserInfoRequest 更新用户信息请求对象
     * @return 操作结果
     */
    @PostMapping("/update_info")
    public Mono<ApiResponse<?>> updateUserInfo(@RequestBody @Validated UpdateUserInfoRequest updateUserInfoRequest) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    try {
                        UUID userId = UUID.fromString(updateUserInfoRequest.getUserId());
                        String username = updateUserInfoRequest.getUsername();
                        String bio = updateUserInfoRequest.getBio();
                        String avatar = updateUserInfoRequest.getAvatar();
                        userService.updateUserInfo(userId, username, bio, avatar);
                    } catch (Exception e) {
                        return ApiResponse.failure(500, "内部服务器错误");
                    }

                    return ApiResponse.success("用户信息更新成功");
                },
                executorService
            )
        );
    }

    /**
     * 更新已使用的存储容量
     * @apiNote 仅内部调用，用于文件系统服务更新用户的已使用存储容量
     * @param updateUsedCapacityRequest 更新已使用存储容量请求对象
     * @return 操作结果
     */
    @PostMapping("/update_used_capacity")
    public Mono<ApiResponse<?>> updateUsedCapacity(@RequestBody @Validated UpdateUsedCapacityRequest updateUsedCapacityRequest) {
        return Mono.fromFuture(
            CompletableFuture.supplyAsync(
                () -> {
                    String userId = updateUsedCapacityRequest.getUserId();
                    BigDecimal usedCapacity = updateUsedCapacityRequest.getUsedCapacity();

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

    /**
     * 查询已使用的存储容量
     * @param userId 用户ID
     * @return 已使用的字节树(byte)
     */
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

    /**
     * 查询用户账号状态
     * @param userId 用户ID
     * @return 操作结果
     */
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

    /**
     * 冻结用户账号
     * @param userId 用户ID
     * @return 操作结果
     */
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

    /**
     * 上传头像
     * @param userId 用户ID
     * @param avatar 头像文件响应流
     * @return 头像的url，只需要在此 url 前方拼接上 minio 的 url 即可获得完整的图片地址
     */
    @PostMapping("/upload_avatar")
    public Mono<ApiResponse<String>> uploadAvatar(@RequestPart("userId") String userId,
                                                  @RequestPart("avatar") Mono<FilePart> avatar) {
        return avatar.flatMap(
            filePart -> {
                    String contentType = Objects.requireNonNull(filePart.headers().getContentType()).toString();
                    if(!contentType.startsWith("image/")) {
                        return Mono.error(new InvalidImageTypeException("无效的文件格式，请选择合适的图片文件"));
                    }

                    return DataBufferUtils.join(filePart.content()) // 将http传输的多个 filePart 合并为 DataBuffer
                    .flatMap(
                       dataBuffer -> {
                            // 将 DataBuffer 转换为 byte[]
                            byte[] fileBytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(fileBytes);
                            DataBufferUtils.release(dataBuffer); // 释放 DataBuffer

                            // 计算文件哈希值
                            String hash = fileHasher.hash(fileBytes);

                            // 构造 MinIO 对象名称
                            String filename = filePart.filename();
                            String extension = filename.substring(filename.lastIndexOf("."));
                            String objectName = userId + "/" + hash + extension;

                            // 创建 InputStream 用于上传
                            InputStream inputStream = new ByteArrayInputStream(fileBytes);

                            // 上传到 MinIO
                            return Mono.fromCallable(
                                () -> {
                                    try {
                                        minioClient.putObject(
                                            PutObjectArgs.builder()
                                                    .bucket(AVATAR_BUCKET)
                                                    .object(objectName)
                                                    .stream(inputStream, fileBytes.length, -1) // 设置输入流、文件大小和偏移量
                                                    .contentType(contentType) // 设置内容类型
                                                    .build()
                                        );

                                        String avatarUrl = AVATAR_BUCKET + "/" + objectName;

                                        userService.updateAvatar(userId, avatarUrl);

                                        return avatarUrl; // 图片的url
                                    } catch (Exception e) {
                                        throw new RuntimeException("头像上传失败");
                                    }
                                }
                            ).subscribeOn(Schedulers.fromExecutorService(executorService));
                       }
                    ).map(ApiResponse::success); // 将图片 url 返回
                }
        ).onErrorResume(
                InvalidImageTypeException.class,
                e -> Mono.just(ApiResponse.failure(400, e.getMessage()))
        ).onErrorResume(
                Exception.class,
                e -> Mono.just(ApiResponse.failure(500, "头像上传失败"))
        );
    }
}
