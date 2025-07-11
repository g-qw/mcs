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
public class MinioFile {
    private String fileId;  // 文件 ID, 实际是 UUID 类型

    @UUID(message = "目录 ID 必须是有效的 UUID 格式")
    private String directoryId;  // 目录 ID, 实际是 UUID 类型

    @NotNull(message = "用户 ID 不能为空")
    @UUID(message = "用户 ID 必须是有效的 UUID 格式")
    private String userId;  // 文件所属用户 ID, 实际是 UUID 类型

    @NotBlank(message = "文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过 255 个字符")
    private String objectName;  // 文件名, 包括文件扩展名，最大长度为 255 个字符

    @NotBlank(message = "文件类型不能为空")
    @Size(max = 128, message = "文件类型长度不能超过 128 个字符")
    private String mimeType;

    @NotNull(message = "文件大小不能为空")
    private Long size;  // 文件大小，单位为字节

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt; // 文件创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt; // 文件最后修改时间

    /**
     * 构造方法, 用于数据库中添加新文件记录
     */
    public MinioFile(String directoryId, String userId, String objectName, Long size) {
        this.directoryId = directoryId;
        this.userId = userId;
        this.objectName = objectName;
        this.size = size;
    }
}
