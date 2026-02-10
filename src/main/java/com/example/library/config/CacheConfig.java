package com.example.library.config;

import java.time.Duration;
import java.util.List;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.example.library.book.dto.BookResponseDTO;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	RedisCacheManager cacheManager(RedisConnectionFactory factory) {

		return RedisCacheManager.builder(factory)

				.withCacheConfiguration("books-by-id",
						RedisCacheConfiguration.defaultCacheConfig()
								.entryTtl(Duration.ofMinutes(10))
								.serializeValuesWith(RedisSerializationContext.SerializationPair
										.fromSerializer(new Jackson2JsonRedisSerializer<>(BookResponseDTO.class))))
				
				.withCacheConfiguration("book-list",
						RedisCacheConfiguration.defaultCacheConfig()
								.entryTtl(Duration.ofMinutes(10))
								.serializeValuesWith(RedisSerializationContext.SerializationPair
										.fromSerializer(new Jackson2JsonRedisSerializer<>(List.class))))
				.build();
	}
}
