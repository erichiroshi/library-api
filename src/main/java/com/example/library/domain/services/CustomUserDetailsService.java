package com.example.library.domain.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.library.domain.entities.User;
import com.example.library.domain.repositories.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomUserDetailsService.class);

	private final UserRepository repository;

	public CustomUserDetailsService(UserRepository userRepository) {
		this.repository = userRepository;
	}

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
