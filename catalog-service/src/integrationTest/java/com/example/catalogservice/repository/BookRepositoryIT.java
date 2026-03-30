package com.example.catalogservice.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.catalogservice.author.Author;
import com.example.catalogservice.book.Book;
import com.example.catalogservice.book.BookRepository;
import com.example.catalogservice.category.Category;
import com.example.catalogservice.common.security.UserContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("it")
@DisplayName("BookRepositoryIT - Integration Tests")
class BookRepositoryIT {

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private TestEntityManager em;
	
	@Mock
	private UserContext userContext;
	
	@Test
	void shouldPersistBook() {

		Category category = em.persist(new Category(null, "Tech"));
		Author author = em.persist(new Author(null, "Eric", "biography"));

		Book book = new Book();
		book.setTitle("Domain-Driven Design");
		book.setIsbn("123456");
		book.setCategory(category);
		book.getAuthors().add(author);

		Book saved = bookRepository.save(book);

		assertNotNull(saved.getId());
	}
}
