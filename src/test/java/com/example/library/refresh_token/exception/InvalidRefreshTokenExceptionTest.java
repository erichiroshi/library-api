package com.example.library.refresh_token.exception;

import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvalidRefreshTokenException - Unit Tests")
class InvalidRefreshTokenExceptionTest {

    @Test
    @DisplayName("Deve criar exceção com token inválido")
    void shouldCreateExceptionWithInvalidToken() {
        // Arrange
        String invalidToken = "abc123-invalid-token";

        // Act
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException(invalidToken);

        // Assert
        assertThat(exception).isNotNull();
        assertThat(exception.getTitle()).isEqualTo("Invalid Refresh Token");
        assertThat(exception.getDetail()).contains("Invalid refresh token");
        assertThat(exception.getDetail()).contains(invalidToken);
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getType()).isEqualTo(URI.create("https://api.library/errors/invalid-refresh-token"));
    }

    @Test
    @DisplayName("Deve incluir token na mensagem de detalhe")
    void shouldIncludeTokenInDetailMessage() {
        // Arrange
        String token = "specific-token-12345";

        // Act
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException(token);

        // Assert
        assertThat(exception.getDetail()).isEqualTo("Invalid refresh token. Token: specific-token-12345");
    }

    @Test
    @DisplayName("Deve ter status HTTP 400 (Bad Request)")
    void shouldHaveBadRequestStatus() {
        // Arrange & Act
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException("any-token");

        // Assert
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exception.getStatus().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve ter URI type específica para documentação")
    void shouldHaveSpecificTypeUri() {
        // Arrange & Act
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException("token");

        // Assert
        assertThat(exception.getType()).hasToString("https://api.library/errors/invalid-refresh-token");
    }

    @Test
    @DisplayName("Deve estender ApiException")
    void shouldExtendApiException() {
        // Arrange & Act
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException("token");

        // Assert
        assertThat(exception).isInstanceOf(com.example.library.common.exception.ApiException.class);
    }

    @Test
    @DisplayName("Deve funcionar com token vazio")
    void shouldHandleEmptyToken() {
        // Arrange & Act
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException("");

        // Assert
        assertThat(exception.getDetail()).isEqualTo("Invalid refresh token. Token: ");
    }

    @Test
    @DisplayName("Deve funcionar com token muito longo")
    void shouldHandleVeryLongToken() {
        // Arrange
        String longToken = "a".repeat(500);

        // Act
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException(longToken);

        // Assert
        assertThat(exception.getDetail()).contains(longToken);
        assertThat(exception.getDetail().length()).isGreaterThan(500);
    }

    @Test
    @DisplayName("Deve funcionar com caracteres especiais no token")
    void shouldHandleSpecialCharactersInToken() {
        // Arrange
        String specialToken = "token-with-special-chars-!@#$%^&*()";

        // Act
        InvalidRefreshTokenException exception = new InvalidRefreshTokenException(specialToken);

        // Assert
        assertThat(exception.getDetail()).contains(specialToken);
        assertThat(exception.getDetail()).contains("!@#$%^&*()");
    }
}