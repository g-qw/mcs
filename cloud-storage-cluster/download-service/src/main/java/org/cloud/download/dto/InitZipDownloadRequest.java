package org.cloud.download.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class InitZipDownloadRequest {
    @NotBlank(message = "bucket 不能为空")
    private String bucket;
    @NotBlank(message = "objects 不能为空")
    private List<String> objects;
}
