package com.example.catalogservice.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Profile("it")
@Configuration
@EnableJpaAuditing
public class JpaTestConfig {

	@Bean
	AuditorAware<String> auditorProvider() {
		return () -> Optional.ofNullable("test-user");
	}
}