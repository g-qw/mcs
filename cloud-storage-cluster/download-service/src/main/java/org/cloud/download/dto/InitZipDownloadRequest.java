package org.cloud.download.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class InitZipDownloadRequest {
    /**
     * 存储桶名称
     */
    @NotBlank(message = "bucket 不能为空")
    private String bucket;

    /**
     * 需要zip打包的文件列表，列表中的元素存储的是文件在 minio 的绝对路径，路径都需要以根目录 / 开始
     */
    @NotEmpty(message = "文件列表不能为空")
    private List<String> files;

    @NotEmpty(message = "文件ID列表不能为空")
    private List<String> fileIds;
}
