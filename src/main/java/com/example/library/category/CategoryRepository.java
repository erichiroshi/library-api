package com.example.library.category;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	Long existsByNameIgnoreCase(String name);

	Optional<Category> findByNameIgnoreCase(String name);

	Page<Category> findAll(Pageable pageable);
}
