package com.example.library.refresh_token.exception;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExpiredRefreshTokenException - Unit Tests")
class ExpiredRefreshTokenExceptionTest {

    @Test
    @DisplayName("Deve criar exceção com data de expiração")
    void shouldCreateExceptionWithExpiryDate() {
        // Arrange
        Instant expireDate = Instant.now().minus(Duration.ofDays(1));

        // Act
        ExpiredRefreshTokenException exception = new ExpiredRefreshTokenException(expireDate);

        // Assert
        assertThat(exception).isNotNull();
        assertThat(exception.getTitle()).isEqualTo("Expired Refresh Token");
        assertThat(exception.getDetail()).contains("Expired refresh token");
        assertThat(exception.getDetail()).contains(expireDate.toString());
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getType()).isEqualTo(URI.create("https://api.library/errors/expired-refresh-token"));
    }

    @Test
    @DisplayName("Deve incluir data de expiração na mensagem de detalhe")
    void shouldIncludeExpireDateInDetailMessage() {
        // Arrange
        Instant expireDate = Instant.parse("2024-01-15T10:30:00Z");

        // Act
        ExpiredRefreshTokenException exception = new ExpiredRefreshTokenException(expireDate);

        // Assert
        assertThat(exception.getDetail()).isEqualTo("Expired refresh token. Expire Date: 2024-01-15T10:30:00Z");
    }

    @Test
    @DisplayName("Deve ter status HTTP 400 (Bad Request)")
    void shouldHaveBadRequestStatus() {
        // Arrange & Act
        ExpiredRefreshTokenException exception = new ExpiredRefreshTokenException(Instant.now());

        // Assert
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getStatus().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve ter URI type específica para documentação")
    void shouldHaveSpecificTypeUri() {
        // Arrange & Act
        ExpiredRefreshTokenException exception = new ExpiredRefreshTokenException(Instant.now());

        // Assert
        assertThat(exception.getType()).hasToString("https://api.library/errors/expired-refresh-token");
    }

    @Test
    @DisplayName("Deve estender ApiException")
    void shouldExtendApiException() {
        // Arrange & Act
        ExpiredRefreshTokenException exception = new ExpiredRefreshTokenException(Instant.now());

        // Assert
        assertThat(exception).isInstanceOf(com.example.library.common.exception.ApiException.class);
    }

    @Test
    @DisplayName("Deve funcionar com data de expiração no passado distante")
    void shouldHandleDistantPastExpiry() {
        // Arrange
        Instant distantPast = Instant.parse("2020-01-01T00:00:00Z");

        // Act
        ExpiredRefreshTokenException exception = new ExpiredRefreshTokenException(distantPast);

        // Assert
        assertThat(exception.getDetail()).contains("2020-01-01T00:00:00Z");
    }

    @Test
    @DisplayName("Deve funcionar com data de expiração no futuro próximo")
    void shouldHandleNearFutureExpiry() {
        // Arrange - expirou há 1 segundo
        Instant justExpired = Instant.now().minus(Duration.ofSeconds(1));

        // Act
        ExpiredRefreshTokenException exception = new ExpiredRefreshTokenException(justExpired);

        // Assert
        assertThat(exception.getDetail()).contains("Expired refresh token");
        assertThat(exception.getDetail()).contains(justExpired.toString());
    }

    @Test
    @DisplayName("Deve formatar corretamente instant com nanossegundos")
    void shouldFormatInstantWithNanoseconds() {
        // Arrange
        Instant withNanos = Instant.parse("2024-06-15T14:30:45.123456789Z");

        // Act
        ExpiredRefreshTokenException exception = new ExpiredRefreshTokenException(withNanos);

        // Assert
        assertThat(exception.getDetail()).contains("2024-06-15T14:30:45.123456789Z");
    }
}