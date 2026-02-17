package com.example.library.repository;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.library.category.Category;
import com.example.library.category.CategoryRepository;

@DataJpaTest
class CategoryRepositoryIT {

	@Autowired
	private CategoryRepository repository;

	@Test
	void shouldSaveCategory() {
		Category category = new Category();
		category.setName("History");

		Category saved = repository.save(category);

		assertNotNull(saved.getId());
	}
}
