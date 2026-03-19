package com.example.library.aws.exception;
 
import static org.assertj.core.api.Assertions.assertThat;
 
import java.net.URI;
 
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
 
@DisplayName("S3UnavailableException")
class S3UnavailableExceptionTest {
 
    @Test
    @DisplayName("Deve criar exceção com status 503 SERVICE_UNAVAILABLE")
    void shouldCreateExceptionWithServiceUnavailableStatus() {
        S3UnavailableException ex = new S3UnavailableException("Cover upload temporarily unavailable.");
 
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
 
    @Test
    @DisplayName("Deve criar exceção com title 'Erro Amazon S3'")
    void shouldCreateExceptionWithCorrectTitle() {
        S3UnavailableException ex = new S3UnavailableException("some detail");
 
        assertThat(ex.getTitle()).isEqualTo("Erro Amazon S3");
    }
 
    @Test
    @DisplayName("Deve criar exceção com detail informado")
    void shouldCreateExceptionWithProvidedDetail() {
        String detail = "Cover upload temporarily unavailable.";
        S3UnavailableException ex = new S3UnavailableException(detail);
 
        assertThat(ex.getDetail()).isEqualTo(detail);
    }
 
    @Test
    @DisplayName("Deve criar exceção com type URI correto")
    void shouldCreateExceptionWithCorrectTypeUri() {
        S3UnavailableException ex = new S3UnavailableException("detail");
 
        assertThat(ex.getType())
                .isEqualTo(URI.create("https://api.library/errors/amazon-S3-error"));
    }
 
    @Test
    @DisplayName("Deve aceitar detail vazio sem lançar exceção")
    void shouldAcceptEmptyDetail() {
        S3UnavailableException ex = new S3UnavailableException("");
 
        assertThat(ex.getDetail()).isEmpty();
    }
}