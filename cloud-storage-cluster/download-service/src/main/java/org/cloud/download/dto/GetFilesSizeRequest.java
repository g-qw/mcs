package org.cloud.download.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class GetFilesSizeRequest {
    /**
     * 文件ID列表
     */
    @NotEmpty
    private List<String> fileIds;
}
