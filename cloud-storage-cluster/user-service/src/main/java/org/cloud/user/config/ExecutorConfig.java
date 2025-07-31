package org.cloud.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ExecutorConfig {

    // 创建一个固定大小的线程池，适用于 I/O 密集型任务
    @Bean
    public ExecutorService executorService() {
        return new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() * 2, // 核心线程数
                Runtime.getRuntime().availableProcessors() * 4, // 最大线程数
                60L,  // 线程空闲时间
                TimeUnit.SECONDS, // 空闲时间单位
                new LinkedBlockingQueue<>(256), // 阻塞队列(可以指定队列大小，避免内存溢出)
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略(当队列满了，新任务会在调用线程中执行)
        );
    }
}
