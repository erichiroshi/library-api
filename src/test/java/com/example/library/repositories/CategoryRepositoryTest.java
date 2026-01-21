package com.example.library.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.library.config.NoCacheTestConfig;
import com.example.library.domain.entities.Category;
import com.example.library.domain.repositories.CategoryRepository;

@ActiveProfiles("test")
@DataJpaTest
@Import(NoCacheTestConfig.class)
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
