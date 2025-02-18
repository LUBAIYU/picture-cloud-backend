package com.by.cloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 自定义线程池配置
 *
 * @author lzh
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * cpu核数
     */
    private final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * 自定义线程池
     *
     * @return ExecutorService
     */
    @Bean
    public ExecutorService threadPoolExecutor() {
        // 自定义线程名称
        ThreadFactory threadFactory = new ThreadFactory() {
            private int count = 1;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "CustomThread-" + count++);
            }
        };

        return new ThreadPoolExecutor(
                CPU_CORES * 2,
                CPU_CORES * 2,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(100),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
