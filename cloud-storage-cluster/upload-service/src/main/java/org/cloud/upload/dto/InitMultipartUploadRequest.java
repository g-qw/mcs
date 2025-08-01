package org.cloud.upload.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
public class InitMultipartUploadRequest {
    /**
     * 存储桶
     */
    @NotBlank(message = "存储桶名称不能为空")
    @JsonProperty(required = true)
    private String bucketName;

    /**
     * 区域名称(可选), 在分布式存储时，可以指定对象的区域
     */
    @JsonProperty()
    private String region;

    /**
     * 上传的文件在 Minio 的绝对路径
     */
    @NotBlank(message = "对象名称不能为空")
    @Size(max = 255, message = "对象名称长度不能超过255个字符")
    @JsonProperty(required = true)
    private String objectName;

    /**
     * 文件的MIME类型
     */
    @NotBlank(message = "内容类型不能为空")
    @Size(max = 64, message = "内容类型长度不能超过64个字符")
    @JsonProperty(required = true)
    private String contentType;
}