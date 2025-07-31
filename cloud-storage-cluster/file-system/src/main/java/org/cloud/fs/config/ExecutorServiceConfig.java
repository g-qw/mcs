package org.cloud.fs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ExecutorServiceConfig {
    public static final String EXECUTOR_BEAN_NAME = "sharedExecutor";

    @Bean(EXECUTOR_BEAN_NAME)
    public Executor sharedExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int processors = Runtime.getRuntime().availableProcessors();

        long maxMemoryBytes   = Runtime.getRuntime().maxMemory(); // -Xmx 指定的最大值
        long bytesPerTask     = 4 * 1024;                         // 预估每个任务的
        int  queueCapacity    = (int) Math.max(1024, maxMemoryBytes / 8 / bytesPerTask);

        executor.setCorePoolSize(processors * 2); // 核心线程数
        executor.setMaxPoolSize(processors * 4); // 最大线程数
        executor.setKeepAliveSeconds(60); // 线程空闲时间
        executor.setQueueCapacity(queueCapacity); // 阻塞队列
        executor.setThreadNamePrefix("shared-"); // 线程名称前缀
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 拒绝策略(当队列满了，新任务会在调用线程中执行)
        executor.initialize(); // 必须调用
        return executor;
    }
}
