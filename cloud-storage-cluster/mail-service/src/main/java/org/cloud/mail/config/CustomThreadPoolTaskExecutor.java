package org.cloud.mail.config;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

public class CustomThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {
    public CustomThreadPoolTaskExecutor() {
        // 获取机器的可用核心数
        int availableProcessors = Runtime.getRuntime().availableProcessors();

        // 核心线程数设置为机器核心数的两倍
        int corePoolSize = availableProcessors * 2;

        // 最大线程数设置为核心线程数的两倍
        int maxPoolSize = corePoolSize * 4;

        // 队列容量可以根据实际需求调整，这里设置为机器核心数的四倍
        int queueCapacity = availableProcessors * 8;

        // 配置线程池
        setCorePoolSize(corePoolSize);  // 核心线程数
        setMaxPoolSize(maxPoolSize);  // 最大线程数
        setQueueCapacity(queueCapacity);  // 队列容量
        setKeepAliveSeconds(60); // 线程空闲时间
        setThreadNamePrefix("mail-thread-"); // 线程名称前缀
        setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略
        afterPropertiesSet(); // 初始化线程池
    }
}

