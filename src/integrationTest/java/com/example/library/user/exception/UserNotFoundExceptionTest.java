package com.example.library.user.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserNotFoundException - Unit Tests")
class UserNotFoundExceptionTest {

    @Test
    @DisplayName("should build exception message with id")
    void shouldBuildExceptionMessageWithId() {
        UserNotFoundException ex = new UserNotFoundException(42L);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getDetail()).contains("42");
    }

    @Test
    @DisplayName("should build exception message with email")
    void shouldBuildExceptionMessageWithEmail() {
        UserNotFoundException ex = new UserNotFoundException("user@email.com");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ex.getDetail()).contains("user@email.com");
    }
}