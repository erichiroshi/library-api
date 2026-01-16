package com.example.library.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.library.domain.entities.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsByNameIgnoreCase(String name);
}
