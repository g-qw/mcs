package org.cloud.user.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class User {
    private String userId;
    private String email; // 邮箱地址（唯一，用于邮箱注册）
    private String username; // 用户名（用户名唯一，可以用于登录）
    private byte[] passwordHash; // 密码哈希（存储哈希值和盐值的组合）
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime lastLoginAt; // 最后登录时间
    private String userStatus; // 用户状态（如激活、禁用等）
    private String role; // 权限级别（如普通用户、管理员等）
    private BigDecimal storageCapacity; // 存储容量（单位：字节）
    private BigDecimal usedCapacity; // 已使用容量（单位：字节）
    private String bio; // 用户简介
    private String avatar; // 用户头像（用于显示）

    public User(String email, String username, byte[] passwordHash) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
    }
}
