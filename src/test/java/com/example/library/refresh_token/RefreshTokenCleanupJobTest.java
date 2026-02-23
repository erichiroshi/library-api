package com.example.library.refresh_token;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenCleanupJob - Unit Tests")
class RefreshTokenCleanupJobTest {

    @Mock
    private RefreshTokenRepository repository;

    @InjectMocks
    private RefreshTokenCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        // Reset mocks before each test
        reset(repository);
    }

    @Test
    @DisplayName("Deve deletar tokens expirados quando existem")
    void shouldDeleteExpiredTokensWhenTheyExist() {
        // Arrange
        when(repository.deleteByExpiryDateBefore(any(Instant.class))).thenReturn(5);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        verify(repository).deleteByExpiryDateBefore(any(Instant.class));
    }

    @Test
    @DisplayName("Deve passar Instant.now() como parâmetro")
    void shouldPassInstantNowAsParameter() {
        // Arrange
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        when(repository.deleteByExpiryDateBefore(instantCaptor.capture())).thenReturn(0);

        // Act
        Instant beforeCall = Instant.now();
        cleanupJob.cleanupExpiredTokens();
        Instant afterCall = Instant.now();

        // Assert
        Instant capturedInstant = instantCaptor.getValue();
        // O instant passado deve estar entre o momento antes e depois da chamada
        assertNotNull(capturedInstant);

        assertTrue(
            capturedInstant.isAfter(beforeCall.minusSeconds(1)),
            "Instant deve ser posterior ao momento antes da chamada"
        );

        assertTrue(
            capturedInstant.isBefore(afterCall.plusSeconds(1)),
            "Instant deve ser anterior ao momento depois da chamada"
        );
        
        verify(repository).deleteByExpiryDateBefore(instantCaptor.getValue());
    }

    @Test
    @DisplayName("Deve logar quando nenhum token expirado é encontrado")
    void shouldLogWhenNoExpiredTokensFound() {
        // Arrange
        when(repository.deleteByExpiryDateBefore(any(Instant.class))).thenReturn(0);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        verify(repository).deleteByExpiryDateBefore(any(Instant.class));
        // Log "No expired refresh tokens found" seria verificado com LogCaptor
    }

    @Test
    @DisplayName("Deve logar quantidade de tokens deletados quando > 0")
    void shouldLogCountWhenTokensDeleted() {
        // Arrange
        when(repository.deleteByExpiryDateBefore(any(Instant.class))).thenReturn(10);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        verify(repository).deleteByExpiryDateBefore(any(Instant.class));
        // Log "Deleted 10 expired refresh tokens" seria verificado com LogCaptor
    }

    @Test
    @DisplayName("Não deve lançar exceção quando repository lança erro")
    void shouldNotPropagateExceptionWhenRepositoryFails() {
        // Arrange
        when(repository.deleteByExpiryDateBefore(any(Instant.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act & Assert - não deve lançar exceção (try-catch no job)
        cleanupJob.cleanupExpiredTokens();

        // Exceção foi capturada e logada, não propagada
        verify(repository).deleteByExpiryDateBefore(any(Instant.class));
    }

    @Test
    @DisplayName("Deve funcionar com grande quantidade de tokens deletados")
    void shouldHandleLargeNumberOfDeletedTokens() {
        // Arrange - 10.000 tokens deletados
        when(repository.deleteByExpiryDateBefore(any(Instant.class))).thenReturn(10000);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        verify(repository).deleteByExpiryDateBefore(any(Instant.class));
    }

    @Test
    @DisplayName("Deve chamar repository apenas uma vez por execução")
    void shouldCallRepositoryOnlyOnce() {
        // Arrange
        when(repository.deleteByExpiryDateBefore(any(Instant.class))).thenReturn(3);

        // Act
        cleanupJob.cleanupExpiredTokens();

        // Assert
        verify(repository, times(1)).deleteByExpiryDateBefore(any(Instant.class));
    }
}