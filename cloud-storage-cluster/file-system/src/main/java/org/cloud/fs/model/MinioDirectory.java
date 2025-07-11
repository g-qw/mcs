package org.cloud.fs.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UUID;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MinioDirectory {
    private String directoryId;  // 目录 ID, 实际是 UUID 类型

    @UUID(message = "父目录 ID 必须是有效的 UUID 格式")
    private String parentDirectoryId; // 父目录 ID，实际是 UUID 类型

    @NotNull(message = "用户 ID 不能为空")
    @UUID(message = "用户 ID 必须是有效的 UUID 格式")
    private String userId;  // 目录所属用户 ID，实际是 UUID 类型

    @NotBlank(message = "目录名称不能为空")
    @Size(max = 255, message = "目录名称长度不能超过 255 个字符")
    private String name;  // 目录名称，最大长度为 255 个字符

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt; // 文件夹创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt; // 文件夹最后更新时间

    /**
     * 构造方法, 用于数据库中添加新目录记录
     * @param parentDirectoryId 父目录 ID
     * @param userId 用户 ID
     * @param name 目录名称
     */
    public MinioDirectory(String parentDirectoryId, String userId, String name) {
        this.parentDirectoryId = parentDirectoryId;
        this.userId = userId;
        this.name = name;
    }

    /**
     * 用于创建根目录，父目录 ID 为 NULL
     * @param userId 用户 ID
     * @param name 目录名称
     */
    public MinioDirectory(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }
}
