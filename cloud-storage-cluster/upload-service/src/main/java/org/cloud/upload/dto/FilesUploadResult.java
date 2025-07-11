package org.cloud.upload.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 多个文件上传结果类, 包含上传成功和失败的文件数量
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class FilesUploadResult {
    private int successCount;
    private int failureCount;
}
