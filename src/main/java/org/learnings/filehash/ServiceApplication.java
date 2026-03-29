package org.learnings.filehash;

import org.learnings.cache.config.CacheConfig;
import org.learnings.filehash.config.ConcurrencyConfigs;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableConfigurationProperties
@Import({ CacheConfig.class, ConcurrencyConfigs.class })
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }
}
