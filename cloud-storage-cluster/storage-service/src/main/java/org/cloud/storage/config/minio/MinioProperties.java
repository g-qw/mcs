package org.cloud.storage.config.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint = "http://127.0.0.1:9000";
    private String accessKey;
    private String secretKey;
    private String storageBucket = "default-storage";
    private String MidiaPreviewBucket = "media-preview";
}
