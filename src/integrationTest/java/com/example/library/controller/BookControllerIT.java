package com.example.library.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.author.Author;
import com.example.library.author.AuthorRepository;
import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.category.Category;
import com.example.library.category.CategoryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@Transactional
@ActiveProfiles("it")
@DisplayName("BookController - Integration Tests")
class BookControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuthorRepository authorRepository;

    private Category category;
    private Author author;
    private Book book;
    
    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Criar categoria
        category = new Category();
        category.setName("Technology");
        category = categoryRepository.save(category);

        // Criar autor
        author = new Author();
        author.setName("Robert C. Martin");
        author = authorRepository.save(author);

        // Criar livro
        book = new Book();
        book.setTitle("Clean Code");
        book.setIsbn("978-0132350884");
        book.setPublicationYear(2008);
        book.setAvailableCopies(5);
        book.setCategory(category);
        book.getAuthors().add(author);
        book = bookRepository.save(book);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CREATE BOOK
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/books - criar livro")
    class CreateBookTests {

        @Test
        @DisplayName("Deve criar livro com sucesso")
        @WithMockUser(roles = "ADMIN")
        void shouldCreateBook() throws Exception {
            // Arrange
            String requestBody = """
                {
                    "title": "Refactoring",
                    "isbn": "978-0134757599",
                    "publicationYear": 2018,
                    "availableCopies": 3,
                    "authorIds": [%d],
                    "categoryId": %d
                }
                """.formatted(author.getId(), category.getId());

            // Act & Assert
            mockMvc.perform(post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Refactoring"))
                .andExpect(jsonPath("$.isbn").value("978-0134757599"));

            // Verificar persistência
            assertThat(bookRepository.existsByIsbn("978-0134757599")).isTrue();
        }

        @Test
        @DisplayName("Deve retornar 409 quando ISBN já existe")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn409WhenIsbnExists() throws Exception {
            // Arrange - usar ISBN do livro existente
            String requestBody = """
                {
                    "title": "Another Book",
                    "isbn": "978-0132350884",
                    "publicationYear": 2020,
                    "availableCopies": 2,
                    "authorIds": [%d],
                    "categoryId": %d
                }
                """.formatted(author.getId(), category.getId());

            // Act & Assert
            mockMvc.perform(post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Book Already Exists"));
        }

        @Test
        @DisplayName("Deve retornar 400 quando authorIds está vazio")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenNoAuthors() throws Exception {
            // Arrange
            String requestBody = """
                {
                    "title": "Book Without Authors",
                    "isbn": "978-1234567890",
                    "publicationYear": 2021,
                    "availableCopies": 1,
                    "authorIds": [],
                    "categoryId": %d
                }
                """.formatted(category.getId());

            // Act & Assert
            mockMvc.perform(post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 404 quando categoria não existe")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenCategoryNotFound() throws Exception {
            // Arrange
            String requestBody = """
                {
                    "title": "Book",
                    "isbn": "978-1234567890",
                    "publicationYear": 2021,
                    "availableCopies": 1,
                    "authorIds": [%d],
                    "categoryId": 999
                }
                """.formatted(author.getId());

            // Act & Assert
            mockMvc.perform(post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Category Not Found"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET BOOK BY ID
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/books/{id} - buscar por ID")
    class GetBookByIdTests {

        @Test
        @DisplayName("Deve retornar livro quando existe")
        @WithMockUser(roles = "USER")
        void shouldReturnBookWhenExists() throws Exception {
            mockMvc.perform(get("/api/v1/books/{id}", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(book.getId()))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.isbn").value("978-0132350884"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando livro não existe")
        @WithMockUser(roles = "USER")
        void shouldReturn404WhenBookNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/books/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Book Not Found"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET ALL BOOKS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/books - listar todos")
    class GetAllBooksTests {

        @Test
        @DisplayName("Deve retornar página de livros")
        @WithMockUser(roles = "USER")
        void shouldReturnPageOfBooks() throws Exception {
            mockMvc.perform(get("/api/v1/books")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não há livros")
        @WithMockUser(roles = "USER")
        void shouldReturnEmptyPageWhenNoBooks() throws Exception {
            // Arrange - deletar o livro criado no setUp
            bookRepository.deleteAll();

            // Act & Assert
            mockMvc.perform(get("/api/v1/books")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Deve retornar 400 quando campo de ordenação inválido")
        @WithMockUser(roles = "USER")
        void shouldReturn400WhenInvalidSortField() throws Exception {
            mockMvc.perform(get("/api/v1/books")
                    .param("page", "0")
                    .param("size", "10")
                    .param("sort", "invalidField,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Sort Field"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // DELETE BOOK
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /api/v1/books/{id} - deletar livro")
    class DeleteBookTests {

        @Test
        @DisplayName("Deve deletar livro com sucesso")
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteBook() throws Exception {
            // Act
            mockMvc.perform(delete("/api/v1/books/{id}", book.getId()))
                .andExpect(status().isNoContent());

            // Assert - verificar que foi deletado
            assertThat(bookRepository.findById(book.getId())).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar 404 ao tentar deletar livro inexistente")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenDeletingNonExistent() throws Exception {
            mockMvc.perform(delete("/api/v1/books/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Book Not Found"));
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário comum tenta deletar")
        @WithMockUser(roles = "USER")
        void shouldReturn403WhenUserTriesToDelete() throws Exception {
            mockMvc.perform(delete("/api/v1/books/{id}", book.getId()))
                .andExpect(status().isForbidden());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CACHE BEHAVIOR (verificar que cache está desabilitado em testes)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Cache - verificar desabilitação em testes")
    class CacheBehaviorTests {

        @Test
        @DisplayName("Deve retornar dados atualizados sem cache")
        @WithMockUser(roles = "USER")
        void shouldReturnFreshDataWithoutCache() throws Exception {
            // Primeira busca
            mockMvc.perform(get("/api/v1/books/{id}", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"));

            // Atualizar o título no banco
            book.setTitle("Clean Code - Updated");
            bookRepository.save(book);

            // Segunda busca - deve retornar o título atualizado (sem cache)
            mockMvc.perform(get("/api/v1/books/{id}", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code - Updated"));
        }
    }
}