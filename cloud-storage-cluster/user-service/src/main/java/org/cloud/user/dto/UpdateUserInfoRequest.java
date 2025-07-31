package org.cloud.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateUserInfoRequest {
    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 用户名称
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 64, message = "用户名长度必须在1到64个字符之间")
    private String username;

    /**
     * 用户简介
     */
    @NotBlank(message = "用户简介不能为空")
    @Size(max = 256, message = "个人简介长度不能超过1024个字符")
    private String bio;

    /**
     * 用户头像地址，在 minio 头像存储桶中的绝对路径
     */
    @NotBlank(message = "用户头像地址不能为空")
    private String avatar;
}
