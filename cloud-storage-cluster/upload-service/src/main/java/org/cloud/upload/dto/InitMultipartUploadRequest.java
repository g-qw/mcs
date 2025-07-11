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
    @NotBlank(message = "存储桶名称不能为空")
    @JsonProperty(required = true)
    private String bucketName;

    @JsonProperty(required = false)
    private String region;

    @NotBlank(message = "对象名称不能为空")
    @Size(max = 255, message = "对象名称长度不能超过255个字符")
    @JsonProperty(required = true)
    private String objectName;

    @NotBlank(message = "内容类型不能为空")
    @Size(max = 64, message = "内容类型长度不能超过64个字符")
    @JsonProperty(required = true)
    private String contentType;
}