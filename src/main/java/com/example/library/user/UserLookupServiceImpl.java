package com.example.library.user;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserLookupServiceImpl implements UserLookupService {

	private final UserRepository repository;

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findById(Long id) {
		return repository.findById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findByEmail(String email) {
		return repository.findByEmail(email);
	}
}