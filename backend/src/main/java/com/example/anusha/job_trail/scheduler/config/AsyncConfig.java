package com.example.anusha.job_trail.scheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Turns on {@code @Async} and gives it a dedicated, bounded pool rather than
 * Spring's default {@link org.springframework.core.task.SimpleAsyncTaskExecutor}
 * (which spins up an unbounded thread per call). Everything running under
 * this pool today is best-effort, per-application background work fired
 * from the scheduler package (e.g. {@code ReminderJob}'s reminder sends) —
 * small, bursty, and never something request threads wait on.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-job-");
        executor.initialize();
        return executor;
    }

    // A void @Async method can't propagate an exception back to its caller —
    // Spring would otherwise just swallow it. Logging it here is the
    // minimum needed to keep a failed reminder send from vanishing silently.
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("Async call to {} failed", method.getName(), ex);
    }
}
