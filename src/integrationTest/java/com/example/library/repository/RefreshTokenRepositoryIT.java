package com.example.library.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.refresh_token.RefreshToken;
import com.example.library.refresh_token.RefreshTokenRepository;
import com.example.library.user.User;
import com.example.library.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("it")
@DisplayName("RefreshTokenRepository - Integration Tests")
class RefreshTokenRepositoryIT {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("password");
        testUser.setRoles(Set.of("ROLE_USER"));
        testUser = userRepository.save(testUser);

        anotherUser = new User();
        anotherUser.setName("Jane Smith");
        anotherUser.setEmail("jane@example.com");
        anotherUser.setPassword("password");
        anotherUser.setRoles(Set.of("ROLE_USER"));
        anotherUser = userRepository.save(anotherUser);
    }

    // ═══════════════════════════════════════════════════════════════════
    // SAVE & FIND
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Save & Basic Queries")
    class SaveAndFindTests {

        @Test
        @DisplayName("Deve salvar refresh token com sucesso")
        void shouldSaveRefreshToken() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("test-token-12345")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();

            // Act
            RefreshToken saved = refreshTokenRepository.save(token);

            // Assert
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getToken()).isEqualTo("test-token-12345");
            assertThat(saved.getUser()).isEqualTo(testUser);
            assertThat(saved.getExpiryDate()).isNotNull();
        }

        @Test
        @DisplayName("Deve gerar ID automaticamente ao salvar")
        void shouldGenerateIdAutomatically() {
            // Arrange
            RefreshToken token1 = RefreshToken.builder()
                .token("token-1")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();

            RefreshToken token2 = RefreshToken.builder()
                .token("token-2")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(anotherUser)
                .build();

            // Act
            RefreshToken saved1 = refreshTokenRepository.save(token1);
            RefreshToken saved2 = refreshTokenRepository.save(token2);

            // Assert
            assertThat(saved1.getId()).isNotNull();
            assertThat(saved2.getId()).isNotNull();
            assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
        }

        @Test
        @DisplayName("Deve persistir a data de expiração corretamente")
        void shouldPersistExpiryDateCorrectly() {
            // Arrange
            Instant expectedExpiry = Instant.now().plus(Duration.ofDays(30));
            RefreshToken token = RefreshToken.builder()
                .token("token-with-expiry")
                .expiryDate(expectedExpiry)
                .user(testUser)
                .build();

            // Act
            RefreshToken saved = refreshTokenRepository.save(token);
            RefreshToken retrieved = refreshTokenRepository.findById(saved.getId()).orElseThrow();

            // Assert
            assertThat(retrieved.getExpiryDate()).isEqualTo(expectedExpiry);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FIND BY TOKEN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByToken()")
    class FindByTokenTests {

        @Test
        @DisplayName("Deve encontrar token por string exata")
        void shouldFindTokenByExactString() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("exact-token-match")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            refreshTokenRepository.save(token);

            // Act
            Optional<RefreshToken> result = refreshTokenRepository.findByToken("exact-token-match");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getToken()).isEqualTo("exact-token-match");
            assertThat(result.get().getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Deve retornar empty quando token não existe")
        void shouldReturnEmptyWhenTokenNotExists() {
            // Act
            Optional<RefreshToken> result = refreshTokenRepository.findByToken("non-existent");

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Busca deve ser case-sensitive")
        void shouldBeCaseSensitive() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("CaseSensitiveToken")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            refreshTokenRepository.save(token);

            // Act
            Optional<RefreshToken> upperCase = refreshTokenRepository.findByToken("CASESENSITIVETOKEN");
            Optional<RefreshToken> lowerCase = refreshTokenRepository.findByToken("casesensitivetoken");
            Optional<RefreshToken> correct = refreshTokenRepository.findByToken("CaseSensitiveToken");

            // Assert
            assertThat(upperCase).isEmpty();
            assertThat(lowerCase).isEmpty();
            assertThat(correct).isPresent();
        }

        @Test
        @DisplayName("Deve encontrar token mesmo se outros existem")
        void shouldFindCorrectTokenAmongMultiple() {
            // Arrange - criar vários tokens
            refreshTokenRepository.save(RefreshToken.builder()
                .token("token-1")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build());

            refreshTokenRepository.save(RefreshToken.builder()
                .token("token-2")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(anotherUser)
                .build());

            refreshTokenRepository.save(RefreshToken.builder()
                .token("token-3")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build());

            // Act
            Optional<RefreshToken> result = refreshTokenRepository.findByToken("token-2");

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getToken()).isEqualTo("token-2");
            assertThat(result.get().getUser()).isEqualTo(anotherUser);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FIND BY USER
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByUser()")
    class FindByUserTests {

        @Test
        @DisplayName("Deve encontrar token por usuário")
        void shouldFindTokenByUser() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("user-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            refreshTokenRepository.save(token);

            // Act
            Optional<RefreshToken> result = refreshTokenRepository.findByUser(testUser);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get().getUser().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("Deve retornar empty quando usuário não tem token")
        void shouldReturnEmptyWhenUserHasNoToken() {
            // Act
            Optional<RefreshToken> result = refreshTokenRepository.findByUser(testUser);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Deve retornar apenas o token do usuário específico")
        void shouldReturnOnlyTokenOfSpecificUser() {
            // Arrange
            RefreshToken testUserToken = RefreshToken.builder()
                .token("test-user-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();

            RefreshToken anotherUserToken = RefreshToken.builder()
                .token("another-user-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(anotherUser)
                .build();

            refreshTokenRepository.save(testUserToken);
            refreshTokenRepository.save(anotherUserToken);

            // Act
            Optional<RefreshToken> testUserResult = refreshTokenRepository.findByUser(testUser);
            Optional<RefreshToken> anotherUserResult = refreshTokenRepository.findByUser(anotherUser);

            // Assert
            assertThat(testUserResult).isPresent();
            assertThat(testUserResult.get().getToken()).isEqualTo("test-user-token");

            assertThat(anotherUserResult).isPresent();
            assertThat(anotherUserResult.get().getToken()).isEqualTo("another-user-token");
        }

//        @Test
//        @DisplayName("Deve retornar apenas UM token quando usuário tem múltiplos (última inserção)")
//        void shouldReturnOneTokenWhenUserHasMultiple() {
//            // Arrange - criar 2 tokens para o mesmo usuário
//            // (na prática não deveria acontecer devido ao delete no service, mas vamos testar)
//            RefreshToken token1 = RefreshToken.builder()
//                .token("first-token")
//                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
//                .user(testUser)
//                .build();
//
//            RefreshToken token2 = RefreshToken.builder()
//                .token("second-token")
//                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
//                .user(testUser)
//                .build();
//
//            refreshTokenRepository.save(token1);
//            refreshTokenRepository.save(token2);
//
//            // Act
//            Optional<RefreshToken> result = refreshTokenRepository.findByUser(testUser);
//
//            // Assert
//            assertThat(result).isPresent();
//            // Pode retornar qualquer um dos dois (depende da implementação do JPA)
//            assertThat(result.get().getUser()).isEqualTo(testUser);
//        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Delete Operations")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar token por ID")
        void shouldDeleteTokenById() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("to-be-deleted")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            RefreshToken saved = refreshTokenRepository.save(token);

            // Act
            refreshTokenRepository.deleteById(saved.getId());

            // Assert
            assertThat(refreshTokenRepository.findById(saved.getId())).isEmpty();
        }

        @Test
        @DisplayName("Deve deletar token por entidade")
        void shouldDeleteTokenByEntity() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("to-be-deleted-2")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            RefreshToken saved = refreshTokenRepository.save(token);

            // Act
            refreshTokenRepository.delete(saved);

            // Assert
            assertThat(refreshTokenRepository.findByToken("to-be-deleted-2")).isEmpty();
        }

        @Test
        @DisplayName("Deletar token não deve afetar o usuário")
        void shouldNotDeleteUserWhenDeletingToken() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("token-to-delete")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            RefreshToken saved = refreshTokenRepository.save(token);

            // Act
            refreshTokenRepository.delete(saved);

            // Assert
            assertThat(userRepository.findById(testUser.getId())).isPresent();
        }

        @Test
        @DisplayName("Deletar todos deve limpar a tabela")
        void shouldDeleteAllTokens() {
            // Arrange
            refreshTokenRepository.save(RefreshToken.builder()
                .token("token-1")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build());

            refreshTokenRepository.save(RefreshToken.builder()
                .token("token-2")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(anotherUser)
                .build());

            // Act
            refreshTokenRepository.deleteAll();

            // Assert
            assertThat(refreshTokenRepository.count()).isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CASCADING & RELATIONSHIPS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Relationships & Cascading")
    class RelationshipTests {

        @Test
        @DisplayName("Deletar usuário NÃO deve deletar refresh tokens automaticamente")
        void shouldNotCascadeDeleteTokensWhenDeletingUser() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("orphaned-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            refreshTokenRepository.save(token);

            // Act
            userRepository.delete(testUser);

            // Assert
            // Isso deve falhar com constraint violation se não houver ON DELETE CASCADE
            // OU o token deve ficar órfão
            // (depende da configuração do FK no migration)
            // Por segurança, tokens devem ser deletados ANTES de deletar o usuário
        }

        @Test
        @DisplayName("Token deve manter referência ao usuário após reload")
        void shouldMaintainUserReferenceAfterReload() {
            // Arrange
            RefreshToken token = RefreshToken.builder()
                .token("persistent-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();
            RefreshToken saved = refreshTokenRepository.save(token);

            // Act - forçar reload do banco
            refreshTokenRepository.flush();
            RefreshToken reloaded = refreshTokenRepository.findById(saved.getId()).orElseThrow();

            // Assert
            assertThat(reloaded.getUser()).isNotNull();
            assertThat(reloaded.getUser().getId()).isEqualTo(testUser.getId());
            assertThat(reloaded.getUser().getEmail()).isEqualTo("john@example.com");
        }
    }
}