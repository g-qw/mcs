package org.cloud.storage.service.impl;

import lombok.RequiredArgsConstructor;
import org.cloud.storage.config.minio.MinioProperties;
import org.cloud.storage.dto.MediaCoverDTO;
import org.cloud.storage.entity.MediaCover;
import org.cloud.storage.repository.MediaCoverRepository;
import org.cloud.storage.service.MediaCoverService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaCoverServiceImpl implements MediaCoverService {
    private final MediaCoverRepository mediaCoverRepository;
    private final MinioProperties minioProperties;

    @Override
    public List<MediaCoverDTO> getMediaCovers(List<UUID> fileIds, UUID userId) {
        return mediaCoverRepository.listByFileIds(fileIds, userId).stream()
                .map(this::buildMediaCoverDTO)
                .toList();
    }

    private MediaCoverDTO buildMediaCoverDTO(MediaCover mediaCover) {
        return MediaCoverDTO.builder()
                .fileId(mediaCover.fileId())
                .url(String.format("%s/%s/%s", minioProperties.getEndpoint(), mediaCover.bucket(), mediaCover.storageKey()))
                .build();
    }
}
