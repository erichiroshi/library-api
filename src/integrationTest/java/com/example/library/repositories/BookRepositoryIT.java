package com.example.library.repositories;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.config.NoCacheTestITConfig;
import com.example.library.domain.entities.Author;
import com.example.library.domain.entities.Book;
import com.example.library.domain.entities.Category;
import com.example.library.domain.repositories.BookRepository;

@Testcontainers
@DataJpaTest
@ActiveProfiles("it")
@Import(NoCacheTestITConfig.class)
class BookRepositoryIT {
	
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
	private BookRepository bookRepository;

	@Test
	void shouldPersistBook() {

		Book book = new Book();
		book.setTitle("Domain-Driven Design");
		book.setIsbn("123456");
		book.getAuthors().add(new Author(1L, "Eric", "biography"));
		book.setCategory(new Category(1L, "Tech"));

		Book saved = bookRepository.save(book);

		assertNotNull(saved.getId());
	}

}
