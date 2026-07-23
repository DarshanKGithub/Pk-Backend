package com.pkcorporate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Step 2 — Bounded async thread pool.
 * <p>
 * Without this, Spring falls back to SimpleAsyncTaskExecutor which creates a new Thread
 * per @Async call and never reuses them. Under burst order creation that causes unbounded
 * thread growth and OOM on Render's 512 MB free tier.
 * <p>
 * Settings are tuned for 0.1 CPU / 512 MB:
 *   - corePoolSize  = 2  (always-live threads for notifications + email)
 *   - maxPoolSize   = 4  (burst headroom)
 *   - queueCapacity = 50 (buffer before rejection)
 *   - CallerRuns on rejection — keeps things safe without dropping tasks
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "taskExecutor")
    @Primary
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.setKeepAliveSeconds(60);
        // If queue is full, run on the caller's thread rather than dropping the task
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return taskExecutor();
    }
}
