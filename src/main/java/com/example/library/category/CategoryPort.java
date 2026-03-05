package com.example.library.category;

import java.util.Optional;

public interface CategoryPort {
	Optional<Category> findById(Long id);
}