package com.example.library.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("it")
public class CacheTestConfig {

	@Bean
	CacheManager cacheManager() {
		return new NoOpCacheManager();
	}
}
