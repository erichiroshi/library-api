package com.example.library.repositories;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.domain.entities.Category;
import com.example.library.domain.repositories.CategoryRepository;

@Testcontainers
@DataJpaTest
@ActiveProfiles("it")
class CategoryRepositoryIT {

    @SuppressWarnings("resource")
	@Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("library_test")
                    .withUsername("test")
                    .withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

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
