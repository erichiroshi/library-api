package com.example.library.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.library.domain.entities.Category;
import com.example.library.domain.repositories.CategoryRepository;

@DataJpaTest
class CategoryRepositoryTest {

	@Autowired
	private CategoryRepository repository;

	@Test
	void shouldSaveCategory() {
		Category category = new Category();
		category.setName("Science");

		Category saved = repository.save(category);

		assertThat(saved.getId()).isNotNull();
	}
}
