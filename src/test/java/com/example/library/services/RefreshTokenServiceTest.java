package com.example.library.services;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.library.refresh_token.RefreshToken;
import com.example.library.refresh_token.RefreshTokenRepository;
import com.example.library.refresh_token.RefreshTokenService;
import com.example.library.refresh_token.exception.ExpiredRefreshTokenException;
import com.example.library.refresh_token.exception.InvalidRefreshTokenException;
import com.example.library.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService - Unit Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Injetar o valor de durationRefreshToken via reflection (simula @Value)
        ReflectionTestUtils.setField(refreshTokenService, "durationRefreshToken", 7L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("password");
        testUser.setRoles(Set.of("ROLE_USER"));
    }

    // ═══════════════════════════════════════════════════════════════════
    // CREATE - Token Creation
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create() - criar refresh token")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("Deve criar novo refresh token quando usuário não tem token anterior")
        void shouldCreateNewTokenWhenUserHasNoExistingToken() {
            // Arrange
            when(repository.findByUser(testUser)).thenReturn(Optional.empty());
            when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
                RefreshToken token = invocation.getArgument(0);
                token.setId(1L);
                return token;
            });

            // Act
            RefreshToken result = refreshTokenService.create(testUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUser()).isEqualTo(testUser);
            assertThat(result.getToken()).isNotNull();
            assertThat(result.getToken().length()).isGreaterThan(70); // UUID duplo
            assertThat(result.getExpiryDate()).isAfter(Instant.now());
            assertThat(result.getExpiryDate()).isBefore(Instant.now().plus(Duration.ofDays(8)));

            verify(repository).findByUser(testUser);
            verify(repository, never()).delete(any()); // Não deve deletar nada
            verify(repository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Deve deletar token antigo antes de criar novo (rotation)")
        void shouldDeleteOldTokenBeforeCreatingNew() {
            // Arrange
            RefreshToken oldToken = RefreshToken.builder()
                .id(1L)
                .token("old-token-12345")
                .expiryDate(Instant.now().plus(Duration.ofDays(5)))
                .user(testUser)
                .build();

            when(repository.findByUser(testUser)).thenReturn(Optional.of(oldToken));
            when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
                RefreshToken token = invocation.getArgument(0);
                token.setId(2L);
                return token;
            });

            // Act
            RefreshToken result = refreshTokenService.create(testUser);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isNotEqualTo("old-token-12345");

            // Verifica ordem de execução: delete ANTES de save
            var inOrder = inOrder(repository);
            inOrder.verify(repository).findByUser(testUser);
            inOrder.verify(repository).delete(oldToken);
            inOrder.verify(repository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Token gerado deve ser único (UUID duplo)")
        void shouldGenerateUniqueToken() {
            // Arrange
            when(repository.findByUser(testUser)).thenReturn(Optional.empty());
            when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            RefreshToken token1 = refreshTokenService.create(testUser);
            RefreshToken token2 = refreshTokenService.create(testUser);

            // Assert
            assertThat(token1.getToken()).isNotEqualTo(token2.getToken());
            assertThat(token1.getToken().length()).isGreaterThan(70);
            assertThat(token2.getToken().length()).isGreaterThan(70);
        }

        @Test
        @DisplayName("Data de expiração deve ser configurável via property")
        void shouldUseConfigurableExpirationDays() {
            // Arrange - alterar para 30 dias
            ReflectionTestUtils.setField(refreshTokenService, "durationRefreshToken", 30L);
            when(repository.findByUser(testUser)).thenReturn(Optional.empty());
            when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            RefreshToken result = refreshTokenService.create(testUser);

            // Assert
            Instant expectedExpiry = Instant.now().plus(Duration.ofDays(30));
            assertThat(result.getExpiryDate())
                .isAfter(Instant.now().plus(Duration.ofDays(29)))
                .isBefore(expectedExpiry.plus(Duration.ofMinutes(1))); // margem de 1 min
        }

        @Test
        @DisplayName("Deve salvar refresh token com user correto")
        void shouldSaveRefreshTokenWithCorrectUser() {
            // Arrange
            when(repository.findByUser(testUser)).thenReturn(Optional.empty());

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            when(repository.save(tokenCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            refreshTokenService.create(testUser);

            // Assert
            RefreshToken savedToken = tokenCaptor.getValue();
            assertThat(savedToken.getUser()).isEqualTo(testUser);
            assertThat(savedToken.getUser().getId()).isEqualTo(1L);
            assertThat(savedToken.getUser().getEmail()).isEqualTo("john@example.com");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // VALIDATE - Token Validation
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validate() - validar refresh token")
    class ValidateRefreshTokenTests {

        @Test
        @DisplayName("Deve retornar token quando válido e não expirado")
        void shouldReturnTokenWhenValidAndNotExpired() {
            // Arrange
            String tokenString = "valid-token-12345";
            RefreshToken validToken = RefreshToken.builder()
                .id(1L)
                .token(tokenString)
                .expiryDate(Instant.now().plus(Duration.ofDays(5)))
                .user(testUser)
                .build();

            when(repository.findByToken(tokenString)).thenReturn(Optional.of(validToken));

            // Act
            RefreshToken result = refreshTokenService.validate(tokenString);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo(tokenString);
            assertThat(result.getUser()).isEqualTo(testUser);

            verify(repository).findByToken(tokenString);
            verify(repository, never()).delete(any()); // Token válido não é deletado
        }

        @Test
        @DisplayName("Deve lançar InvalidRefreshTokenException quando token não existe")
        void shouldThrowInvalidRefreshTokenExceptionWhenNotFound() {
            // Arrange
            String invalidToken = "non-existent-token";
            when(repository.findByToken(invalidToken)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.validate(invalidToken))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("Invalid refresh token")
                .hasMessageContaining(invalidToken);

            verify(repository).findByToken(invalidToken);
            verify(repository, never()).delete(any());
        }

        @Test
        @DisplayName("Deve lançar ExpiredRefreshTokenException e deletar quando expirado")
        void shouldThrowExpiredRefreshTokenExceptionAndDeleteWhenExpired() {
            // Arrange
            String expiredTokenString = "expired-token-12345";
            Instant pastExpiry = Instant.now().minus(Duration.ofDays(1));
            
            RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token(expiredTokenString)
                .expiryDate(pastExpiry)
                .user(testUser)
                .build();

            when(repository.findByToken(expiredTokenString)).thenReturn(Optional.of(expiredToken));

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.validate(expiredTokenString))
                .isInstanceOf(ExpiredRefreshTokenException.class)
                .hasMessageContaining("Expired refresh token")
                .hasMessageContaining(pastExpiry.toString());

            // Verifica que o token expirado foi deletado do banco
            verify(repository).delete(expiredToken);
        }

        @Test
        @DisplayName("Deve aceitar token que expira exatamente agora (edge case)")
        void shouldAcceptTokenExpiringRightNow() {
            // Arrange - token expira em 1 segundo (ainda não expirado)
            String tokenString = "almost-expired-token";
            Instant almostExpired = Instant.now().plus(Duration.ofSeconds(1));
            
            RefreshToken token = RefreshToken.builder()
                .id(1L)
                .token(tokenString)
                .expiryDate(almostExpired)
                .user(testUser)
                .build();

            when(repository.findByToken(tokenString)).thenReturn(Optional.of(token));

            // Act
            RefreshToken result = refreshTokenService.validate(tokenString);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getToken()).isEqualTo(tokenString);
            verify(repository, never()).delete(any()); // Ainda não expirado, não deleta
        }

        @Test
        @DisplayName("Deve rejeitar token expirado há exatamente 1 segundo")
        void shouldRejectTokenExpiredOneSecondAgo() {
            // Arrange - expirou há 1 segundo
            String tokenString = "just-expired-token";
            Instant justExpired = Instant.now().minus(Duration.ofSeconds(1));
            
            RefreshToken token = RefreshToken.builder()
                .id(1L)
                .token(tokenString)
                .expiryDate(justExpired)
                .user(testUser)
                .build();

            when(repository.findByToken(tokenString)).thenReturn(Optional.of(token));

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.validate(tokenString))
                .isInstanceOf(ExpiredRefreshTokenException.class);

            verify(repository).delete(token);
        }

        @Test
        @DisplayName("Validação deve retornar o user associado ao token")
        void shouldReturnUserAssociatedWithToken() {
            // Arrange
            User anotherUser = new User();
            anotherUser.setId(2L);
            anotherUser.setEmail("another@example.com");

            String tokenString = "user2-token";
            RefreshToken token = RefreshToken.builder()
                .id(2L)
                .token(tokenString)
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(anotherUser)
                .build();

            when(repository.findByToken(tokenString)).thenReturn(Optional.of(token));

            // Act
            RefreshToken result = refreshTokenService.validate(tokenString);

            // Assert
            assertThat(result.getUser()).isEqualTo(anotherUser);
            assertThat(result.getUser().getId()).isEqualTo(2L);
            assertThat(result.getUser().getEmail()).isEqualTo("another@example.com");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // EDGE CASES & SECURITY
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Edge Cases & Security")
    class EdgeCasesTests {

        @Test
        @DisplayName("Não deve aceitar token null")
        void shouldNotAcceptNullToken() {
            // Arrange
            when(repository.findByToken(null)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.validate(null))
                .isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        @DisplayName("Não deve aceitar token vazio")
        void shouldNotAcceptEmptyToken() {
            // Arrange
            when(repository.findByToken("")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenService.validate(""))
                .isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        @DisplayName("Token rotation deve garantir que cada create() gera token diferente")
        void shouldEnsureTokenRotationGeneratesDifferentTokens() {
            // Arrange
            RefreshToken existingToken = RefreshToken.builder()
                .id(1L)
                .token("existing-token")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();

            when(repository.findByUser(testUser)).thenReturn(Optional.of(existingToken));
            when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            RefreshToken newToken = refreshTokenService.create(testUser);

            // Assert
            assertThat(newToken.getToken()).isNotEqualTo("existing-token");
            verify(repository).delete(existingToken); // Token antigo deletado
        }

        @Test
        @DisplayName("Múltiplas chamadas de create() para o mesmo usuário devem sempre deletar o anterior")
        void shouldAlwaysDeletePreviousTokenOnMultipleCreates() {
            // Arrange
            RefreshToken token1 = RefreshToken.builder()
                .id(1L)
                .token("token-1")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();

            RefreshToken token2 = RefreshToken.builder()
                .id(2L)
                .token("token-2")
                .expiryDate(Instant.now().plus(Duration.ofDays(7)))
                .user(testUser)
                .build();

            when(repository.findByUser(testUser))
                .thenReturn(Optional.empty())      // 1ª chamada: sem token
                .thenReturn(Optional.of(token1))   // 2ª chamada: tem token1
                .thenReturn(Optional.of(token2));  // 3ª chamada: tem token2

            when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            refreshTokenService.create(testUser); // Cria token1
            refreshTokenService.create(testUser); // Cria token2, deleta token1
            refreshTokenService.create(testUser); // Cria token3, deleta token2

            // Assert
            verify(repository, times(2)).delete(any(RefreshToken.class)); // Deletou token1 e token2
            verify(repository, times(3)).save(any(RefreshToken.class));   // Salvou 3 tokens
        }
    }
}