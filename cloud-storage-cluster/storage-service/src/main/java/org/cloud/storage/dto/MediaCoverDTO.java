package org.cloud.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "媒体文件的封面")
public class MediaCoverDTO implements Serializable {
    @Schema(description = "文件ID")
    private UUID fileId;

    @Schema(description = "封面的访问地址")
    private String url;
}
