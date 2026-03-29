package org.learnings.cache.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class CacheConfig {

    @Bean
    public ThreadPoolTaskExecutor ttlCacheExecutor() {
        log.debug("Starting thread pool task executor: cacheExecutor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cache-thread-pool-");

        executor.initialize();

        return executor;
    }
}
