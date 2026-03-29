package org.learnings.filehash.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
public class ConcurrencyConfigs {

    @Bean
    public ThreadPoolTaskExecutor stringFrequencyPipelineExecutor() {
        log.debug("Starting thread pool task executor: stringFrequencyPipelineExecutor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);

        executor.initialize();

        return executor;
    }
}
