package org.cloud.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloud.user.dto.ApiResponse;
import org.cloud.user.dto.UserInfo;
import org.cloud.user.exception.UserNotFound;
import org.cloud.user.model.User;
import org.cloud.user.mappers.UserMapper;
import org.cloud.user.util.secure.PasswordHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserService {
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserMapper userMapper;
    private final PasswordHasher passwordHasher;
    private final WebClient webClient;

    public UserService(UserMapper userMapper, PasswordHasher passwordHasher, WebClient webClient) {
        this.userMapper = userMapper;
        this.passwordHasher = passwordHasher;
        this.webClient = webClient;
    }

    public Mono<String> register(String email, String username, String password)  {
        // 检查邮箱是否已注册
        if (userMapper.selectByEmail(email) != null) {
            throw new RuntimeException("邮箱已注册");
        }

        // 对密码明文进行加密
        byte[] encryptedPassword = null;
        try {
            encryptedPassword = passwordHasher.hashPassword(password);
        } catch (Exception e) {
            logger.error("密码加密出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }

        // 创建用户
        User user = new User(email, username, encryptedPassword);
        userMapper.insertUser(user);

        // 创建用户的根目录
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http") // 指定协议
                        .host("localhost") // 指定主机
                        .port(8104) // 指定端口
                        .path("/root_dir") // 指定路径
                        .queryParam("userId", user.getUserId()) // 添加请求参数
                        .build())
                .retrieve() // 发起请求并获取响应
                .bodyToMono(ApiResponse.class) // 将响应体转换为 ApiResponse 类型
                .flatMap(response -> {
                    if (response.getCode() == 200) {
                        return Mono.just(user.getUserId());
                    } else {
                        userMapper.deleteUser(UUID.fromString(user.getUserId()));  // 撤销用户的注册
                        return Mono.empty();
                    }
                });
    }

    public String login(String email, String password) {
        User user = userMapper.selectByEmail(email);

        // 检查用户是否存在
        if (user == null) {
            throw new UserNotFound();
        }

        // 检查用户是否被冻结
        if (user.getUserStatus().equals("FROZEN")) {
            throw new RuntimeException("用户已被冻结");
        }

        // 验证密码
        try {
            // 验证密码
            boolean isValid = passwordHasher.verifyPassword(password, user.getPasswordHash());

            // 登录失败
            if(!isValid) return null;

            // 登录成功，记录登录时间
            userMapper.updateLastLoginAt(email);

            // 登录成功，返回用户id
            return user.getUserId();
        } catch (Exception e) {
            logger.error("密码验证出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }
    }

    public UserInfo getUserInfo(String userId) {
        User user = userMapper.selectById(UUID.fromString(userId));
        return new UserInfo(user);
    }

    public boolean resetPassword(String email, String newPassword) {
        // 更新密码
        try {
            byte[] newPasswordHash = passwordHasher.hashPassword(newPassword);
            userMapper.updatePassword(email, newPasswordHash);
            return true;
        } catch (Exception e) {
            logger.error("密码加密出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }
    }

    public boolean updatePassword(String email, String password, String newPassword) {
        // 检查密码是否正确
        User user = userMapper.selectByEmail(email);
        boolean isValid;
        try {
            isValid = passwordHasher.verifyPassword(password, user.getPasswordHash());
        } catch (Exception e) {
            logger.error("密码验证出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }

        // 密码不正确
        if(!isValid)  return false;

        // 检查密码不能重复
        if(password.equals(newPassword)) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }

        // 更新密码
        try {
            byte[] newPasswordHash = passwordHasher.hashPassword(newPassword);
            userMapper.updatePassword(email, newPasswordHash);
            return true;
        } catch (Exception e) {
            logger.error("密码加密出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }
    }

    public void updateUserInfo(UserInfo userInfo) {
        try {
            userMapper.updateUser(userInfo);
        } catch (Exception e) {
            logger.error("更新用户信息出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }
    }

    public void updateUsedCapacity(String userId, BigDecimal usedCapacity) {
        try {
            userMapper.updateUsedCapacity(UUID.fromString(userId), usedCapacity);
        } catch (Exception e) {
            logger.error("更新已使用容量出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }
    }

    public BigDecimal getUsedCapacity(String userId) {
        try {
            return userMapper.selectUsedCapacity(UUID.fromString(userId));
        } catch (Exception e) {
            logger.error("查询已使用容量出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }
    }

    public String selectUserStatus(String userId) {
        try {
            return userMapper.selectUserStatus(UUID.fromString(userId));
        } catch (Exception e) {
            logger.error("查询用户状态出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }
    }

    public void freezeUser(String userId) {
        try {
            userMapper.updateUserStatus(UUID.fromString(userId), "FROZEN");
        } catch (Exception e) {
            logger.error("冻结用户出现错误", e);
            throw new RuntimeException("内部服务器错误");
        }
    }
}
