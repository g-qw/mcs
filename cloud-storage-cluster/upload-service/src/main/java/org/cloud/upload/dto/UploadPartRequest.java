package org.cloud.upload.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UploadPartRequest {
    @NotBlank(message = "存储桶名称不能为空")
    private String bucketName;

    private String region;

    @NotBlank(message = "对象名称不能为空")
    private String objectName;

    @NotBlank(message = "上传 ID 不能为空")
    private String uploadId;

    @NotNull(message = "分段编号不能为空")
    private int partNumber;
}
