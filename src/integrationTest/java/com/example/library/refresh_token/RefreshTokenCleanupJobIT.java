package com.example.library.refresh_token;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.user.User;
import com.example.library.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("it")
@Transactional
@DisplayName("RefreshTokenCleanupJob - Integration Tests")
class RefreshTokenCleanupJobIT {

    @Autowired
    private RefreshTokenCleanupJob cleanupJob;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("password");
        testUser.setRoles(Set.of("ROLE_USER"));
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Deve deletar apenas tokens expirados, mantendo os válidos")
    void shouldDeleteOnlyExpiredTokensKeepingValidOnes() {
        // Arrange - criar 3 tokens expirados e 2 válidos
        RefreshToken expired1 = createToken("expired-1", Instant.now().minus(Duration.ofDays(10)));
        RefreshToken expired2 = createToken("expired-2", Instant.now().minus(Duration.ofDays(5)));
        RefreshToken expired3 = createToken("expired-3", Instant.now().minus(Duration.ofSeconds(1)));
        
        RefreshToken valid1 = createToken("valid-1", Instant.now().plus(Duration.ofDays(7)));
        RefreshToken valid2 = createToken("valid-2", Instant.now().plus(Duration.ofDays(14)));

        refreshTokenRepository.save(expired1);
        refreshTokenRepository.save(expired2);
        refreshTokenRepository.save(expired3);
        refreshTokenRepository.save(valid1);
        refreshTokenRepository.save(valid2);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        assertThat(refreshTokenRepository.count()).isEqualTo(2); // Apenas os 2 válidos
        assertThat(refreshTokenRepository.findByToken("expired-1")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("expired-2")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("expired-3")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("valid-1")).isPresent();
        assertThat(refreshTokenRepository.findByToken("valid-2")).isPresent();
    }

    @Test
    @DisplayName("Não deve deletar nada quando todos os tokens são válidos")
    void shouldNotDeleteWhenAllTokensAreValid() {
        // Arrange
        RefreshToken valid1 = createToken("valid-1", Instant.now().plus(Duration.ofDays(7)));
        RefreshToken valid2 = createToken("valid-2", Instant.now().plus(Duration.ofDays(14)));
        
        refreshTokenRepository.save(valid1);
        refreshTokenRepository.save(valid2);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve deletar todos quando todos os tokens estão expirados")
    void shouldDeleteAllWhenAllTokensAreExpired() {
        // Arrange
        RefreshToken expired1 = createToken("expired-1", Instant.now().minus(Duration.ofDays(1)));
        RefreshToken expired2 = createToken("expired-2", Instant.now().minus(Duration.ofDays(2)));
        RefreshToken expired3 = createToken("expired-3", Instant.now().minus(Duration.ofDays(3)));
        
        refreshTokenRepository.save(expired1);
        refreshTokenRepository.save(expired2);
        refreshTokenRepository.save(expired3);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("Não deve fazer nada quando não há tokens no banco")
    void shouldDoNothingWhenNoTokensExist() {
        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve manter token que expira exatamente agora (edge case)")
    void shouldKeepTokenExpiringRightNow() {
        // Arrange - token expira daqui 1 segundo (ainda não expirado)
        RefreshToken almostExpired = createToken("almost-expired", Instant.now().plus(Duration.ofSeconds(1)));
        refreshTokenRepository.save(almostExpired);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        assertThat(refreshTokenRepository.findByToken("almost-expired")).isPresent();
    }

    @Test
    @DisplayName("Deve deletar token expirado há exatamente 1 segundo")
    void shouldDeleteTokenExpiredOneSecondAgo() {
        // Arrange - expirou há 1 segundo
        RefreshToken justExpired = createToken("just-expired", Instant.now().minus(Duration.ofSeconds(1)));
        refreshTokenRepository.save(justExpired);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        assertThat(refreshTokenRepository.findByToken("just-expired")).isEmpty();
    }

    @Test
    @DisplayName("Deve funcionar corretamente com grande volume de tokens")
    void shouldHandleLargeVolumeOfTokens() {
        // Arrange - criar 100 tokens (50 expirados, 50 válidos)
        for (int i = 0; i < 50; i++) {
            RefreshToken expired = createToken("expired-" + i, Instant.now().minus(Duration.ofDays(i + 1)));
            refreshTokenRepository.save(expired);
        }

        for (int i = 0; i < 50; i++) {
            RefreshToken valid = createToken("valid-" + i, Instant.now().plus(Duration.ofDays(i + 1)));
            refreshTokenRepository.save(valid);
        }

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        assertThat(refreshTokenRepository.count()).isEqualTo(50); // Apenas os válidos
    }

    @Test
    @DisplayName("Múltiplas execuções do job não devem causar problemas")
    void shouldHandleMultipleExecutionsSafely() {
        // Arrange
        RefreshToken expired = createToken("expired", Instant.now().minus(Duration.ofDays(1)));
        RefreshToken valid = createToken("valid", Instant.now().plus(Duration.ofDays(7)));
        
        refreshTokenRepository.save(expired);
        refreshTokenRepository.save(valid);

        // Act - executar 3 vezes
        cleanupJob.cleanupExpiredTokens();
        cleanupJob.cleanupExpiredTokens();
        cleanupJob.cleanupExpiredTokens();

        // Assert - resultado deve ser idempotente
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
        assertThat(refreshTokenRepository.findByToken("valid")).isPresent();
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════════════

    private RefreshToken createToken(String token, Instant expiryDate) {
        return RefreshToken.builder()
            .token(token)
            .expiryDate(expiryDate)
            .user(testUser)
            .build();
    }
}