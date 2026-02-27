package com.example.library.aws.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImageProcessingService - Unit Tests")
class ImageProcessingServiceTest {

    private ImageProcessingService service;
    private final int maxWidth = 400;

    @BeforeEach
    void setUp() {
        service = new ImageProcessingService();
    }

    @Test
    @DisplayName("Deve comprimir imagem PNG para JPEG com largura reduzida")
    void shouldCompressPngToJpegWithReducedWidth() throws Exception {
        // Arrange
        BufferedImage original = createTestImage(800, 600);
        byte[] imageBytes = imageToBytes(original, "png");
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.png", "image/png", imageBytes
        );

        // Act
        MultipartFile compressed = service.compressImage(file, maxWidth);

        // Assert
        assertThat(compressed).isNotNull();
        assertThat(compressed.getContentType()).isEqualTo("image/jpeg");
        
        BufferedImage result = ImageIO.read(compressed.getInputStream());
        assertThat(result.getWidth()).isEqualTo(maxWidth);
        assertThat(result.getHeight()).isEqualTo(300); // Mantém aspect ratio (800x600 → 400x300)
    }

    @Test
    @DisplayName("Deve manter aspect ratio ao comprimir")
    void shouldMaintainAspectRatio() throws Exception {
        // Arrange
        BufferedImage original = createTestImage(1000, 500); // 2:1 ratio
        byte[] imageBytes = imageToBytes(original, "png");
        MockMultipartFile file = new MockMultipartFile(
            "file", "wide.png", "image/png", imageBytes
        );

        // Act
        MultipartFile compressed = service.compressImage(file, maxWidth);

        // Assert
        BufferedImage result = ImageIO.read(compressed.getInputStream());
        assertThat(result.getWidth()).isEqualTo(maxWidth);
        assertThat(result.getHeight()).isEqualTo(200); // 2:1 ratio mantido
    }

    @Test
    @DisplayName("Deve lançar IOException para arquivo corrompido")
    void shouldThrowIOExceptionForCorruptedFile() {
        // Arrange
        MockMultipartFile corruptedFile = new MockMultipartFile(
            "file", "corrupt.png", "image/png", "not-an-image".getBytes()
        );

        // Act & Assert
        assertThatThrownBy(() -> service.compressImage(corruptedFile, maxWidth))
            .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Deve comprimir imagem JPEG")
    void shouldCompressJpegImage() throws Exception {
        // Arrange
        BufferedImage original = createTestImage(600, 400);
        byte[] imageBytes = imageToBytes(original, "jpg");
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", imageBytes
        );

        // Act
        MultipartFile compressed = service.compressImage(file, maxWidth);

        // Assert
        assertThat(compressed).isNotNull();
        assertThat(compressed.getContentType()).isEqualTo("image/jpeg");
        
        BufferedImage result = ImageIO.read(compressed.getInputStream());
        assertThat(result.getWidth()).isEqualTo(maxWidth);
    }

    // Helper methods
    private BufferedImage createTestImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    private byte[] imageToBytes(BufferedImage image, String format) throws Exception {
        var baos = new java.io.ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}