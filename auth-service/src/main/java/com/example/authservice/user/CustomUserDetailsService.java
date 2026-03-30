package com.example.authservice.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomUserDetailsService.class);

	private final UserRepository repository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		User user = repository.findByEmail(username).orElseThrow(() -> {
			log.error("User not found: {}", username);
			throw new UsernameNotFoundException("User not found");
		});

		log.info("User found: {}", username);

		return user;
	}
}
