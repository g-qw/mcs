package org.cloud.storage.service;

import org.cloud.storage.dto.MediaCoverDTO;

import java.util.List;
import java.util.UUID;

public interface MediaCoverService {
    List<MediaCoverDTO> getMediaCovers(List<UUID> fileIds, UUID userId);
}
