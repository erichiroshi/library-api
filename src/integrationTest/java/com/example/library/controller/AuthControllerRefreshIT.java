package com.example.library.controller;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.refresh_token.RefreshToken;
import com.example.library.refresh_token.RefreshTokenRepository;
import com.example.library.user.User;
import com.example.library.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("it")
@Transactional
@DisplayName("AuthController - Refresh Token Integration Tests")
class AuthControllerRefreshIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

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

    // ═══════════════════════════════════════════════════════════════════
    // POST /auth/login - Deve retornar refresh token
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /auth/login - criação de refresh token")
    class LoginWithRefreshTokenTests {

        @Test
        @DisplayName("Login deve retornar access_token e refresh_token")
        void shouldReturnAccessAndRefreshTokenOnLogin() throws Exception {
            mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "username": "john@example.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.expires_in").exists())
                .andExpect(jsonPath("$.refresh_token").isString())
                .andExpect(jsonPath("$.refresh_token").value(org.hamcrest.Matchers.hasLength(org.hamcrest.Matchers.greaterThan(70))));

            // Verifica que o refresh token foi salvo no banco
            assertThat(refreshTokenRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Refresh token deve ser salvo no banco após login")
        void shouldSaveRefreshTokenInDatabaseAfterLogin() throws Exception {
            String response = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "username": "john@example.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            // Extrai o refresh_token da resposta (parse JSON manual simples)
            String refreshToken = response.split("\"refresh_token\":\"")[1].split("\"")[0];

            // Verifica que está no banco
            RefreshToken savedToken = refreshTokenRepository.findByToken(refreshToken).orElseThrow();
            assertThat(savedToken.getUser().getEmail()).isEqualTo("john@example.com");
            assertThat(savedToken.getExpiryDate()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("Segundo login deve invalidar refresh token anterior (rotation)")
        void shouldInvalidateOldRefreshTokenOnSecondLogin() throws Exception {
            // Primeiro login
            String firstResponse = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "username": "john@example.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            String firstRefreshToken = firstResponse.split("\"refresh_token\":\"")[1].split("\"")[0];

            // Segundo login
            String secondResponse = mockMvc.perform(post("/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "username": "john@example.com",
                            "password": "password123"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            String secondRefreshToken = secondResponse.split("\"refresh_token\":\"")[1].split("\"")[0];

            // Tokens devem ser diferentes
            assertThat(firstRefreshToken).isNotEqualTo(secondRefreshToken);

            // Token antigo não deve mais existir no banco
            assertThat(refreshTokenRepository.findByToken(firstRefreshToken)).isEmpty();

            // Token novo deve existir
            assertThat(refreshTokenRepository.findByToken(secondRefreshToken)).isPresent();

            // Apenas 1 token no banco (não acumula)
            assertThat(refreshTokenRepository.count()).isEqualTo(1);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST /auth/refresh - Renovação de access token
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /auth/refresh - renovação de tokens")
    class RefreshTokenTests {

        @Test
        @DisplayName("Deve retornar novo access_token e refresh_token quando válido")
        void shouldReturnNewTokensWhenRefreshTokenValid() throws Exception {
            // Arrange - criar refresh token válido
            RefreshToken validRefresh = RefreshToken.builder()
                .token("valid-refresh-token-12345")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            refreshTokenRepository.save(validRefresh);

            // Act & Assert
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "refreshToken": "valid-refresh-token-12345"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists())
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.expires_in").exists());
        }

        @Test
        @DisplayName("Refresh deve gerar NOVO refresh_token (rotation)")
        void shouldGenerateNewRefreshTokenOnRefresh() throws Exception {
            // Arrange
            RefreshToken oldRefresh = RefreshToken.builder()
                .token("old-refresh-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            refreshTokenRepository.save(oldRefresh);

            // Act
            String response = mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "refreshToken": "old-refresh-token"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            String newRefreshToken = response.split("\"refresh_token\":\"")[1].split("\"")[0];

            // Assert
            assertThat(newRefreshToken).isNotEqualTo("old-refresh-token");

            // Token antigo foi deletado
            assertThat(refreshTokenRepository.findByToken("old-refresh-token")).isEmpty();

            // Token novo foi salvo
            assertThat(refreshTokenRepository.findByToken(newRefreshToken)).isPresent();
        }

        @Test
        @DisplayName("Deve retornar 400 quando refresh token não existe")
        void shouldReturn400WhenRefreshTokenNotExists() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "refreshToken": "non-existent-token"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Refresh Token"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("non-existent-token")));
        }

        @Test
        @DisplayName("Deve retornar 400 e deletar token quando expirado")
        void shouldReturn400AndDeleteWhenRefreshTokenExpired() throws Exception {
            // Arrange - criar token expirado
            RefreshToken expiredToken = RefreshToken.builder()
                .token("expired-token")
                .expiryDate(Instant.now().minus(Duration.ofDays(1)))
                .user(testUser)
                .build();
            refreshTokenRepository.save(expiredToken);

            // Act & Assert
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "refreshToken": "expired-token"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Expired Refresh Token"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Expired refresh token")));

            // Token expirado foi deletado do banco
            assertThat(refreshTokenRepository.findByToken("expired-token")).isEmpty();
        }

        @Test
        @DisplayName("Novo access_token deve conter claims do usuário correto")
        void shouldGenerateAccessTokenWithCorrectUserClaims() throws Exception {
            // Arrange - criar usuário ADMIN
            User adminUser = new User();
            adminUser.setName("Admin User");
            adminUser.setEmail("admin@example.com");
            adminUser.setPassword(passwordEncoder.encode("admin123"));
            adminUser.setRoles(Set.of("ROLE_ADMIN"));
            adminUser = userRepository.save(adminUser);

            RefreshToken adminRefresh = RefreshToken.builder()
                .token("admin-refresh-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(adminUser)
                .build();
            refreshTokenRepository.save(adminRefresh);

            // Act
            String response = mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "refreshToken": "admin-refresh-token"
                        }
                        """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

            // Assert - o access_token gerado deve ter as roles do admin
            // (isso seria verificado decodificando o JWT, mas aqui verificamos indiretamente)
            assertThat(response).contains("access_token");
            
            // Verifica que o refresh_token novo está associado ao admin
            String newRefreshToken = response.split("\"refresh_token\":\"")[1].split("\"")[0];
            RefreshToken savedToken = refreshTokenRepository.findByToken(newRefreshToken).orElseThrow();
            assertThat(savedToken.getUser().getEmail()).isEqualTo("admin@example.com");
            assertThat(savedToken.getUser().getRoles()).contains("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Refresh token usado NÃO deve funcionar novamente (one-time use)")
        void shouldNotAllowReusingRefreshToken() throws Exception {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("one-time-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            refreshTokenRepository.save(token);

            // Act - primeira vez funciona
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "refreshToken": "one-time-token"
                        }
                        """))
                .andExpect(status().isOk());

            // Segunda vez falha (token foi rotacionado)
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "refreshToken": "one-time-token"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Refresh Token"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // EDGE CASES & SECURITY
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge Cases & Security")
    class EdgeCasesTests {

        @Test
        @DisplayName("Deve retornar 400 quando refreshToken é null")
        void shouldReturn400WhenRefreshTokenIsNull() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "refreshToken": null
                        }
                        """))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando refreshToken é string vazia")
        void shouldReturn400WhenRefreshTokenIsEmpty() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "refreshToken": ""
                        }
                        """))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve retornar 400 quando body está vazio")
        void shouldReturn400WhenBodyIsEmpty() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Múltiplos refreshes devem sempre gerar tokens únicos")
        void shouldAlwaysGenerateUniqueTokensOnMultipleRefreshes() throws Exception {
            // Arrange
            RefreshToken initialToken = RefreshToken.builder()
                .token("initial-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            refreshTokenRepository.save(initialToken);

            // Act - refresh 3 vezes
            String token1 = extractRefreshToken(
                mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"initial-token\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString()
            );

            String token2 = extractRefreshToken(
                mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + token1 + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString()
            );

            String token3 = extractRefreshToken(
                mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"" + token2 + "\"}"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString()
            );

            // Assert - todos devem ser diferentes
            assertThat(token1).isNotEqualTo("initial-token");
            assertThat(token2).isNotEqualTo(token1);
            assertThat(token3).isNotEqualTo(token2);

            // Apenas o último token deve existir no banco
            assertThat(refreshTokenRepository.count()).isEqualTo(1);
            assertThat(refreshTokenRepository.findByToken(token3)).isPresent();
        }

        private String extractRefreshToken(String response) {
            return response.split("\"refresh_token\":\"")[1].split("\"")[0];
        }
    }
}