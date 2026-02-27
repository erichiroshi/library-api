package com.example.library.book.exception;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvalidOperationException - Unit Tests")
class InvalidOperationExceptionTest {

    @Test
    @DisplayName("Deve criar exceção sem argumentos")
    void shouldCreateExceptionWithoutArgs() {
        // Act
        InvalidOperationException exception = new InvalidOperationException();

        // Assert
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getTitle()).isNotNull();
        assertThat(exception.getDetail()).isNotNull();
        assertThat(exception.getStatus()).isNotNull();
    }

    @Test
    @DisplayName("Deve criar exceção com Set de IDs")
    void shouldCreateExceptionWithSetOfIds() {
        // Arrange
        Set<Long> invalidIds = Set.of(1L, 2L, 3L);

        // Act
        InvalidOperationException exception = new InvalidOperationException(invalidIds);

        // Assert
        assertThat(exception.getDetail()).contains("1", "2", "3");
        assertThat(exception.getTitle()).isEqualTo("Invalid Operation");
        assertThat(exception.getStatus().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Deve incluir todos os IDs na mensagem de erro")
    void shouldIncludeAllIdsInErrorMessage() {
        // Arrange
        Set<Long> ids = Set.of(10L, 20L, 30L, 40L);

        // Act
        InvalidOperationException exception = new InvalidOperationException(ids);

        // Assert
        String detail = exception.getDetail();
        assertThat(detail).contains("10");
        assertThat(detail).contains("20");
        assertThat(detail).contains("30");
        assertThat(detail).contains("40");
    }
}