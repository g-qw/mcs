package org.cloud.mail.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "mailExecutor")
    public CustomThreadPoolTaskExecutor threadPoolTaskExecutor() {
        return new CustomThreadPoolTaskExecutor();
    }
}
