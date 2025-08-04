package org.cloud.download.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ZipDownloadRequest {
    /**
     * zip下载任务ID
     */
    @NotBlank(message = "zip下载任务ID不能为空")
    private String zipTaskId;

    /**
     * zip目标文件名称，以 .zip 结尾
     */
    @JsonProperty(defaultValue = "target.zip")
    private String target;
}
