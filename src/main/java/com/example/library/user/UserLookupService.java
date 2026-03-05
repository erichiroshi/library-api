package com.example.library.user;

import java.util.Optional;

public interface UserLookupService {
	Optional<User> findById(Long id);

	Optional<User> findByEmail(String email);
}