package org.cloud.user.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.cloud.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@Data
public class UserInfo {
    private String userId;
    private String username;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private String userStatus;
    private String role;
    private BigDecimal storageCapacity;
    private BigDecimal usedCapacity;
    private String bio;
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
