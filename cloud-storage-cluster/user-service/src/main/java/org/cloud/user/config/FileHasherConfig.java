package org.cloud.user.config;

import org.cloud.user.util.file.FileHasher;
import org.cloud.user.util.file.impl.MD5FileHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileHasherConfig {

    @Bean
    FileHasher MD5FileHasher() {
        return new MD5FileHasher();
    }
}
