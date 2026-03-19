package com.example.library.category;

import java.util.Optional;

public interface CategoryLookupService {
	Optional<Category> findById(Long id);
}