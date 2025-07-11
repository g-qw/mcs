package org.cloud.fs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class DirectoryCreationResponse {
    private String directoryId;
    private String path;
}
