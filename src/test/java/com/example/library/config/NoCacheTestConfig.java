package com.example.library.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class NoCacheTestConfig {

    @Bean
    CacheManager cacheManager() {
        return new NoOpCacheManager();
    }
}
