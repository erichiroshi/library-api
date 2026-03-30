package com.example.library.aws.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomMultipartFile - Unit Tests")
class CustomMultipartFileTest {

    @Test
    @DisplayName("Deve criar CustomMultipartFile com sucesso")
    void shouldCreateCustomMultipartFile() {
        // Arrange
        byte[] content = "test content".getBytes();

        // Act
        CustomMultipartFile file = new CustomMultipartFile(
            "testFile", "original.txt", "text/plain", content
        );

        // Assert
        assertThat(file.getName()).isEqualTo("testFile");
        assertThat(file.getOriginalFilename()).isEqualTo("original.txt");
        assertThat(file.getContentType()).isEqualTo("text/plain");
        assertThat(file.getSize()).isEqualTo(content.length);
        assertThat(file.getBytes()).isEqualTo(content);
        assertThat(file.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("isEmpty() deve retornar true para conteúdo vazio")
    void shouldReturnTrueForEmptyContent() {
        // Arrange
        CustomMultipartFile file = new CustomMultipartFile(
            "empty", "empty.txt", "text/plain", new byte[0]
        );

        // Act & Assert
        assertThat(file.isEmpty()).isTrue();
        assertThat(file.getSize()).isZero();
    }

    @Test
    @DisplayName("isEmpty() deve retornar true para conteúdo null")
    void shouldReturnTrueForNullContent() {
        // Arrange
        CustomMultipartFile file = new CustomMultipartFile(
            "null", "null.txt", "text/plain", null
        );

        // Act & Assert
        assertThat(file.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("getInputStream() deve retornar stream com conteúdo correto")
    void shouldReturnInputStreamWithCorrectContent() throws IOException {
        // Arrange
        byte[] content = "test data".getBytes();
        CustomMultipartFile file = new CustomMultipartFile(
            "stream", "file.txt", "text/plain", content
        );

        // Act
        byte[] streamContent = file.getInputStream().readAllBytes();

        // Assert
        assertThat(streamContent).isEqualTo(content);
    }

    @Test
    @DisplayName("transferTo() deve escrever conteúdo em arquivo destino")
    void shouldTransferContentToDestinationFile() throws IOException {
        // Arrange
        byte[] content = "transfer test".getBytes();
        CustomMultipartFile file = new CustomMultipartFile(
            "transfer", "source.txt", "text/plain", content
        );
        File dest = File.createTempFile("dest", ".txt");
        dest.deleteOnExit();

        // Act
        file.transferTo(dest);

        // Assert
        byte[] fileContent = Files.readAllBytes(dest.toPath());
        assertThat(fileContent).isEqualTo(content);
    }
}