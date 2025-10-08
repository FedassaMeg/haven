package org.haven.reporting.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Spring @Async configuration for report generation
 * Dedicated thread pool separate from transaction pool to prevent blocking
 *
 * Thread pool sizing follows HUD export workload characteristics:
 * - Core pool: 2 threads (handles typical concurrent exports)
 * - Max pool: 8 threads (scales for APR/CAPER season spikes)
 * - Queue: 50 (allows queuing during peak periods)
 * - Keep-alive: 60s (reclaim idle threads)
 */
@Configuration
@EnableAsync
public class ReportingAsyncConfiguration {

    @Bean(name = "reportGenerationExecutor")
    public TaskExecutor reportGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - minimum threads always alive
        executor.setCorePoolSize(2);

        // Maximum pool size - max concurrent report generation tasks
        executor.setMaxPoolSize(8);

        // Queue capacity - pending export jobs
        executor.setQueueCapacity(50);

        // Thread name prefix for monitoring/debugging
        executor.setThreadNamePrefix("hud-export-");

        // Keep-alive time for idle threads above core pool size
        executor.setKeepAliveSeconds(60);

        // Rejection policy when queue is full
        // CallerRunsPolicy: calling thread executes task if pool is saturated
        // This provides graceful degradation vs failing the export
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    /**
     * Separate executor for validation tasks to prevent validation from blocking generation
     */
    @Bean(name = "reportValidationExecutor")
    public TaskExecutor reportValidationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("hud-validation-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}
