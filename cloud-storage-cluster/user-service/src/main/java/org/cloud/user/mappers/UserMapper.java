package org.cloud.user.mappers;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.cloud.user.dto.UpdateUserInfoRequest;
import org.cloud.user.dto.UserInfo;
import org.cloud.user.model.User;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper
public interface UserMapper{
    // 根据用户ID查询用户
    User selectById(@Param("userId") UUID userId);

    // 根据邮箱查询用户
    User selectByEmail(@Param("email") String email);

    // 根据用户名查询用户
    User selectByUsername(@Param("username") String username);

    // 查询已使用容类
    BigDecimal selectUsedCapacity(@Param("userId") UUID userId);

    // 查询用户状态
    String selectUserStatus(@Param("userId") UUID userId);

    // 根据邮箱查询用户是否存在
    Boolean isUserExist(@Param("email") String email);

    // 插入新用户
    int insertUser(User user);

    // 更新上一次登录时间
    int updateLastLoginAt(@Param("email") String email);

    // 更新用户信息
    int updateUserInfo(UUID userId, String username, String bio, String avatar);

    // 更新密码
    int updatePassword(@Param("email") String email,
                       @Param("passwordHash") byte[] passwordHash);

    // 更新已使用容量
    int updateUsedCapacity(@Param("userId") UUID userId,
                           @Param("usedCapacity") BigDecimal usedCapacity);

    // 更新用户状态
    int updateUserStatus(@Param("userId") UUID userId,
                         @Param("userStatus") String userStatus);

    int updateAvatar(@Param("userId") UUID userId, @Param("avatar") String avatar);

    // 删除用户
    int deleteUser(@Param("userId") UUID userId);
}
