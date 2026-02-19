package com.example.library.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.library.security.filter.JwtAuthenticationFilter;

@Profile("dev")
@Configuration
public class ResourceSecurityDev {

	
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) {
	    http.csrf(AbstractHttpConfigurer::disable) // NOSONAR - API stateless com JWT, CSRF não se aplica	     
	    	.sessionManagement(session ->
	            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
	        )
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/auth/**", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated()
	        )
	        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

	    return http.build();
	}

}
