package org.cloud.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class FileCreationResponse {
    private String fileId; // 文件的 ID
    private String path; // 文件的绝对路径
}
