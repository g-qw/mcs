package org.cloud.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.cloud.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@Data
public class UserInfo {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 账号的邮箱地址
     */
    private String email;

    /**
     * 账号创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 账号上一次登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 用户状态
     */
    private String userStatus;

    /**
     * 用户角色
     */
    private String role;

    /**
     * 存储空间，单位字节(byte)
     */
    private BigDecimal storageCapacity;

    /**
     * 已使用的存储空间，单位字节(byte)
     */
    private BigDecimal usedCapacity;

    /**
     * 用户简介
     */
    private String bio;

    /**
     * 用户头像 url
     */
    private String avatar;

    public UserInfo(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.createdAt = user.getCreatedAt();
        this.lastLoginAt = user.getLastLoginAt();
        this.userStatus = user.getUserStatus();
        this.role = user.getRole();
        this.storageCapacity = user.getStorageCapacity();
        this.usedCapacity = user.getUsedCapacity();
        this.bio = user.getBio();
        this.avatar = user.getAvatar();
    }
}
