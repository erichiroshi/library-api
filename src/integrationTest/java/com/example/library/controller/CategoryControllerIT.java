package com.example.library.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.category.Category;
import com.example.library.category.CategoryRepository;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Testcontainers
@ActiveProfiles("it")
@Sql(
		scripts = "/cleanup.sql", 
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("CategoryController - Integration Tests")
class CategoryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;
    
    private Category category;
    
    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        category = new Category();
        category.setName("Technology");
        category = categoryRepository.save(category);
    }

    @Nested
    @DisplayName("POST /api/categories")
    class CreateTests {

        @Test
        @DisplayName("ADMIN deve criar categoria com sucesso")
        @WithMockUser(roles = "ADMIN")
        void shouldCreateCategory() throws Exception {
            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name": "Science"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Science"));

            assertThat(categoryRepository.findByNameIgnoreCase("Science")).isPresent();
        }

        @Test
        @DisplayName("Deve retornar 409 quando categoria já existe")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn409WhenExists() throws Exception {
            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name": "Technology"}
                        """))
                .andExpect(status().isConflict());
        }
        
        @Test
        @DisplayName("Deve retornar 400 quando nome está em branco")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenNameBlank() throws Exception {
            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name": ""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.name").exists());
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário comum tenta criar")
        @WithMockUser(roles = "USER")
        void shouldReturn403ForUser() throws Exception {
            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"name": "Science"}
                        """))
                .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/categories/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Deve retornar categoria quando existe")
        @WithMockUser
        void shouldReturnCategory() throws Exception {
            mockMvc.perform(get("/api/categories/{id}", category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Technology"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando não existe")
        @WithMockUser
        void shouldReturn404() throws Exception {
            mockMvc.perform(get("/api/categories/{id}", 999L))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/categories")
    class GetAllTests {

        @Test
        @DisplayName("Deve listar categorias paginadas")
        @WithMockUser
        void shouldListCategories() throws Exception {
            mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Technology"));
        }
        
        @Test
        @DisplayName("Deve retornar página vazia quando não há categorias")
        @WithMockUser
        void shouldReturnEmptyPage() throws Exception {
            categoryRepository.deleteAll();

            mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("DELETE /api/categories/{id}")
    class DeleteTests {

        @Test
        @DisplayName("ADMIN deve deletar categoria com sucesso")
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteCategory() throws Exception {
            mockMvc.perform(delete("/api/categories/{id}", category.getId()))
                .andExpect(status().isNoContent());

            assertThat(categoryRepository.findById(category.getId())).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar 404 ao deletar categoria inexistente")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenDeleting() throws Exception {
            mockMvc.perform(delete("/api/categories/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Category Not Found"));
        }

        @Test
        @DisplayName("Deve retornar 403 quando usuário comum tenta deletar")
        @WithMockUser(roles = "USER")
        void shouldReturn403ForUser() throws Exception {
            mockMvc.perform(delete("/api/categories/{id}", category.getId()))
                .andExpect(status().isForbidden());
        }
    }
}