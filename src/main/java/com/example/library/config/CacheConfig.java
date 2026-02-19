package com.example.library.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
@Profile("!it")
public class CacheConfig {

	@Bean
	RedisCacheManager cacheManager(RedisConnectionFactory factory) {

		return RedisCacheManager.builder(factory)
				.cacheDefaults(
						RedisCacheConfiguration.defaultCacheConfig()
						.entryTtl(Duration.ofMinutes(2)))
				.build();
	}
}
