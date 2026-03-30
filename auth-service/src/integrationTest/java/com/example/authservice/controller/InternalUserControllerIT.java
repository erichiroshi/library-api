package com.example.authservice.controller;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.authservice.user.User;
import com.example.authservice.user.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@ActiveProfiles("it")
@DisplayName("InternalUserController - Integration Tests")
class InternalUserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRoles(Set.of("ROLE_USER"));
        testUser = userRepository.save(testUser);
    }

    @Nested
    @DisplayName("GET /internal/users/{id}")
    class FindByIdTests {

        @Test
        @DisplayName("Deve retornar usuário quando existe")
        void shouldReturnUserWhenExists() throws Exception {
            mockMvc.perform(get("/internal/users/{id}", testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando usuário não existe")
        void shouldReturn404WhenNotFound() throws Exception {
            mockMvc.perform(get("/internal/users/{id}", 999L))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /internal/users/by-email")
    class FindByEmailTests {

        @Test
        @DisplayName("Deve retornar usuário quando email existe")
        void shouldReturnUserWhenEmailExists() throws Exception {
            mockMvc.perform(get("/internal/users/by-email")
                    .param("email", "john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando email não existe")
        void shouldReturn404WhenEmailNotFound() throws Exception {
            mockMvc.perform(get("/internal/users/by-email")
                    .param("email", "notfound@example.com"))
                .andExpect(status().isNotFound());
        }
    }
}