package org.cloud.storage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cloud.storage.dto.ApiResponse;
import org.cloud.storage.dto.MediaCoverDTO;
import org.cloud.storage.dto.MediaCoverRequest;
import org.cloud.storage.service.MediaCoverService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage/cover")
@RequiredArgsConstructor
@Tag(name = "媒体封面 API", description = "支持批量获取媒体封面")
public class MediaCoverController {
    private final MediaCoverService mediaCoverService;

    @PostMapping
    @Operation(summary = "批量获取媒体封面")
    ApiResponse<List<MediaCoverDTO>> getMediaCovers(
            @RequestBody MediaCoverRequest request,
            @Parameter(description = "用户 ID") @RequestHeader(value = "UID") UUID uid) {
        return ApiResponse.success(mediaCoverService.getMediaCovers(request.getFileIds(), uid));
    }
}
