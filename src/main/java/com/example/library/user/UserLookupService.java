package com.example.library.user;

import java.util.Optional;

public interface UserLookupService {
	Optional<User> findById(Long id);
}