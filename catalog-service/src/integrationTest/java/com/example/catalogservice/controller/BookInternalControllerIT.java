package com.example.catalogservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.catalogservice.author.Author;
import com.example.catalogservice.author.AuthorRepository;
import com.example.catalogservice.book.Book;
import com.example.catalogservice.book.BookRepository;
import com.example.catalogservice.category.Category;
import com.example.catalogservice.category.CategoryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@Transactional
@ActiveProfiles("it")
@DisplayName("BookController - Internal Endpoints (decrement/restore)")
class BookInternalControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    private Book book;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setName("Technology");
        category = categoryRepository.save(category);

        Author author = new Author();
        author.setName("Robert C. Martin");
        author = authorRepository.save(author);

        book = new Book();
        book.setTitle("Clean Code");
        book.setIsbn("978-0132350884");
        book.setPublicationYear(2008);
        book.setAvailableCopies(5);
        book.setCategory(category);
        book.getAuthors().add(author);
        book = bookRepository.save(book);
    }

    @Nested
    @DisplayName("PATCH /api/v1/books/{id}/decrement")
    class DecrementTests {

        @Test
        @DisplayName("Deve decrementar cópias disponíveis")
        @WithMockUser
        void shouldDecrementCopies() throws Exception {
            mockMvc.perform(patch("/api/v1/books/{id}/decrement", book.getId()))
                .andExpect(status().isOk());

            Book updated = bookRepository.findById(book.getId()).orElseThrow();
            assertThat(updated.getAvailableCopies()).isEqualTo(4);
        }

        @Test
        @DisplayName("Deve retornar 0 quando não há cópias disponíveis")
        @WithMockUser
        void shouldReturnZeroWhenNoCopies() throws Exception {
            book.setAvailableCopies(0);
            bookRepository.save(book);

            mockMvc.perform(patch("/api/v1/books/{id}/decrement", book.getId()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve retornar 404 quando livro não existe")
        @WithMockUser
        void shouldReturn404WhenBookNotFound() throws Exception {
            mockMvc.perform(patch("/api/v1/books/{id}/decrement", 999L))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/books/{id}/restore/{quantity}")
    class RestoreTests {

        @Test
        @DisplayName("Deve restaurar cópias disponíveis")
        @WithMockUser
        void shouldRestoreCopies() throws Exception {
            mockMvc.perform(patch("/api/v1/books/{id}/restore/{quantity}", book.getId(), 3))
                .andExpect(status().isNoContent());

            Book updated = bookRepository.findById(book.getId()).orElseThrow();
            assertThat(updated.getAvailableCopies()).isEqualTo(8);
        }

        @Test
        @DisplayName("Deve retornar 404 quando livro não existe")
        @WithMockUser
        void shouldReturn404WhenBookNotFound() throws Exception {
            mockMvc.perform(patch("/api/v1/books/{id}/restore/{quantity}", 999L, 1))
                .andExpect(status().isNotFound());
        }
    }
}