package com.example.catalogservice.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.example.catalogservice.common.security.UserContext;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
public class JpaConfig {

	private final UserContext userContext;

	@Bean
	AuditorAware<String> auditorProvider() {
		return () -> Optional.ofNullable(userContext.getUserId());
	}
}