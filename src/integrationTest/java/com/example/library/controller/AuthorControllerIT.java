package com.example.library.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.author.Author;
import com.example.library.author.AuthorRepository;

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
@DisplayName("AuthorController - Integration Tests")
class AuthorControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthorRepository authorRepository;

    private Author author;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setName("Robert C. Martin");
        author.setBiography("Autor e engenheiro de software");
        author = authorRepository.save(author);
    }

    @Nested
    @DisplayName("POST /api/authors")
    class CreateTests {

        @Test
        @DisplayName("Deve criar autor com sucesso")
        @WithMockUser(roles = "ADMIN")
        void shouldCreateAuthor() throws Exception {
            mockMvc.perform(post("/api/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "Martin Fowler",
                            "biography": "Autor e engenheiro de software"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Martin Fowler"))
                .andExpect(jsonPath("$.biography").value("Autor e engenheiro de software"));

            assertThat(authorRepository.count()).isEqualTo(2);
        }

        @Test
        @DisplayName("Deve criar autor sem biografia")
        @WithMockUser(roles = "ADMIN")
        void shouldCreateAuthorWithoutBirthDate() throws Exception {
            mockMvc.perform(post("/api/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": "Unknown Author"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Unknown Author"))
                .andExpect(jsonPath("$.birthDate").doesNotExist());
        }

        @Test
        @DisplayName("Deve retornar 400 quando nome está em branco")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenNameBlank() throws Exception {
            mockMvc.perform(post("/api/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "name": ""
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
        }
    }

    @Nested
    @DisplayName("GET /api/authors/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Deve retornar autor quando existe")
        @WithMockUser
        void shouldReturnAuthor() throws Exception {
            mockMvc.perform(get("/api/authors/{id}", author.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(author.getId()))
                .andExpect(jsonPath("$.name").value("Robert C. Martin"))
                .andExpect(jsonPath("$.biography").value("Autor e engenheiro de software"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando autor não existe")
        @WithMockUser
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(get("/api/authors/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Author Not Found"));
        }
    }

    @Nested
    @DisplayName("GET /api/authors")
    class GetAllTests {

        @Test
        @DisplayName("Deve listar autores com paginação")
        @WithMockUser
        void shouldListAuthors() throws Exception {
            mockMvc.perform(get("/api/authors")
                    .param("page", "0")
                    .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Robert C. Martin"))
                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não há autores")
        @WithMockUser
        void shouldReturnEmptyPage() throws Exception {
            authorRepository.deleteAll();

            mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("DELETE /api/authors/{id}")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar autor com sucesso")
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteAuthor() throws Exception {
            mockMvc.perform(delete("/api/authors/{id}", author.getId()))
                .andExpect(status().isNoContent());

            assertThat(authorRepository.findById(author.getId())).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar 404 ao deletar autor inexistente")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenDeleting() throws Exception {
            mockMvc.perform(delete("/api/authors/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Author Not Found"));
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário comum tenta deletar")
        @WithMockUser(roles = "USER")
        void shouldReturn403ForUser() throws Exception {
            mockMvc.perform(delete("/api/authors/{id}", author.getId()))
                .andExpect(status().isForbidden());
        }
    }
}