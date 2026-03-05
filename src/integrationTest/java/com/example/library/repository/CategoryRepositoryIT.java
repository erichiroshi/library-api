package com.example.library.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.category.Category;
import com.example.library.category.CategoryRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@DisplayName("CategoryRepositoryIT - Integration Tests")
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
