package com.example.library.aws;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import com.example.library.aws.exception.AmazonClientException;
import com.example.library.aws.exception.URIException;
import com.example.library.aws.utils.ImageProcessingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Testes unitários para S3Service
 * VERSÃO ATUALIZADA com ImageProcessingService
 * 
 * PARTE 1/2: Setup + Upload Success + File Size Validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("S3Service - Unit Tests (Updated)")
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Utilities s3Utilities;
    
    @Mock
    private ImageProcessingService imageProcessingService;

    private S3Service s3Service;

    private final String bucketName = "library-api-s3";
    private final String folder = "books/";
    private final String fileName = "book-1";
    private final int maxWidth = 400;

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, imageProcessingService);
        
        ReflectionTestUtils.setField(s3Service, "bucketName", bucketName);
        ReflectionTestUtils.setField(s3Service, "maxWidth", maxWidth);
    }

    // ═══════════════════════════════════════════════════════════════════
    // UPLOAD SUCCESS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadFile() - Casos de Sucesso")
    class UploadFileSuccessTests {

        @Test
        @DisplayName("Deve fazer upload de PNG (comprimido para JPEG)")
        void shouldUploadPngImageSuccessfully() throws Exception {
            MockMultipartFile original = new MockMultipartFile(
                "file", "cover.png", "image/png", createValidImageBytes()
            );
            
            MockMultipartFile compressed = new MockMultipartFile(
                "file", "cover.png", "image/jpeg", "compressed".getBytes()
            );

            URI expectedUri = URI.create("https://library-api-s3.s3.sa-east-1.amazonaws.com/books/book-1.jpg");

            when(imageProcessingService.compressImage(original, maxWidth)).thenReturn(compressed);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
            when(s3Client.utilities()).thenReturn(s3Utilities);
            when(s3Utilities.getUrl(any(GetUrlRequest.class))).thenReturn(expectedUri.toURL());

            URI result = s3Service.uploadFile(original, folder, fileName);

            assertThat(result).isEqualTo(expectedUri);
            verify(imageProcessingService).compressImage(original, maxWidth);

            ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

            PutObjectRequest request = captor.getValue();
            assertThat(request.bucket()).isEqualTo(bucketName);
            assertThat(request.key()).isEqualTo(folder + fileName + ".jpg");
            assertThat(request.contentType()).isEqualTo("image/jpeg");
            assertThat(request.metadata()).containsKey("uploaded-by");
        }

        @Test
        @DisplayName("Deve incluir folder na GetUrlRequest (bug fix)")
        void shouldIncludeFolderInGetUrlRequest() throws Exception {
            MockMultipartFile original = new MockMultipartFile(
                "file", "cover.png", "image/png", createValidImageBytes()
            );
            
            MockMultipartFile compressed = new MockMultipartFile(
                "file", "cover.png", "image/jpeg", "data".getBytes()
            );

            URI expectedUri = URI.create("https://s3.amazonaws.com/books/book-1.jpg");

            when(imageProcessingService.compressImage(original, maxWidth)).thenReturn(compressed);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
            when(s3Client.utilities()).thenReturn(s3Utilities);
            when(s3Utilities.getUrl(any(GetUrlRequest.class))).thenReturn(expectedUri.toURL());

            s3Service.uploadFile(original, folder, fileName);

            ArgumentCaptor<GetUrlRequest> captor = ArgumentCaptor.forClass(GetUrlRequest.class);
            verify(s3Utilities).getUrl(captor.capture());
            
            assertThat(captor.getValue().key()).isEqualTo(folder + fileName + ".jpg");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FILE SIZE VALIDATION
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validação de Tamanho")
    class ValidateFileSizeTests {

        @Test
        @DisplayName("Deve rejeitar arquivo < 1KB")
        void shouldRejectFileTooSmall() throws IOException {
            MockMultipartFile tiny = new MockMultipartFile(
                "file", "tiny.png", "image/png", new byte[100]
            );

            assertThatThrownBy(() -> s3Service.uploadFile(tiny, folder, fileName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File too small");

            verify(imageProcessingService, never()).compressImage(any(), anyInt());
        }

        @Test
        @DisplayName("Deve rejeitar arquivo > 10MB")
        void shouldRejectFileTooLarge() throws IOException {
            MockMultipartFile large = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[11 * 1024 * 1024]
            );

            assertThatThrownBy(() -> s3Service.uploadFile(large, folder, fileName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File too large");

            verify(imageProcessingService, never()).compressImage(any(), anyInt());
        }

        @Test
        @DisplayName("Deve aceitar arquivo de exatos 1KB")
        void shouldAcceptMinimumSize() throws Exception {
            MockMultipartFile min = new MockMultipartFile(
                "file", "min.png", "image/png", new byte[1024]
            );
            
            MockMultipartFile compressed = new MockMultipartFile(
                "file", "min.png", "image/jpeg", "data".getBytes()
            );

            when(imageProcessingService.compressImage(min, maxWidth)).thenReturn(compressed);
            setupSuccessfulUpload();

            s3Service.uploadFile(min, folder, fileName);

            verify(imageProcessingService).compressImage(min, maxWidth);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════════════

    private byte[] createValidImageBytes() {
        return new byte[2048]; // 2KB
    }

    private void setupSuccessfulUpload() throws Exception {
        URI uri = URI.create("https://s3.amazonaws.com/books/book-1.jpg");
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(GetUrlRequest.class))).thenReturn(uri.toURL());
    }
    
    /**
     * PARTE 2/2: Content Type Validation + Error Handling
     * 
     * Continue from Part 1...
     */

    // ═══════════════════════════════════════════════════════════════════
    // CONTENT TYPE VALIDATION
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validação de Content Type")
    class ValidateContentTypeTests {

        @Test
        @DisplayName("Deve rejeitar content type nulo")
        void shouldRejectNullContentType() {
            MockMultipartFile nullType = new MockMultipartFile(
                "file", "cover.png", null, createValidImageBytes()
            );

            assertThatThrownBy(() -> s3Service.uploadFile(nullType, folder, fileName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid content type");
        }

        @Test
        @DisplayName("Deve rejeitar PDF")
        void shouldRejectPdf() {
            MockMultipartFile pdf = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[10000]
            );

            assertThatThrownBy(() -> s3Service.uploadFile(pdf, folder, fileName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("application/pdf");
        }

        @Test
        @DisplayName("Deve aceitar image/png")
        void shouldAcceptPng() throws Exception {
            MockMultipartFile png = new MockMultipartFile(
                "file", "cover.png", "image/png", createValidImageBytes()
            );
            MockMultipartFile compressed = new MockMultipartFile(
                "file", "cover.png", "image/jpeg", "data".getBytes()
            );

            when(imageProcessingService.compressImage(png, maxWidth)).thenReturn(compressed);
            setupSuccessfulUpload();

            s3Service.uploadFile(png, folder, fileName);
            verify(imageProcessingService).compressImage(png, maxWidth);
        }

        @Test
        @DisplayName("Deve aceitar image/jpeg")
        void shouldAcceptJpeg() throws Exception {
            MockMultipartFile jpeg = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", createValidImageBytes()
            );
            MockMultipartFile compressed = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", "data".getBytes()
            );

            when(imageProcessingService.compressImage(jpeg, maxWidth)).thenReturn(compressed);
            setupSuccessfulUpload();

            s3Service.uploadFile(jpeg, folder, fileName);
            verify(imageProcessingService).compressImage(jpeg, maxWidth);
        }

        @Test
        @DisplayName("Deve aceitar image/webp")
        void shouldAcceptWebp() throws Exception {
            MockMultipartFile webp = new MockMultipartFile(
                "file", "cover.webp", "image/webp", createValidImageBytes()
            );
            MockMultipartFile compressed = new MockMultipartFile(
                "file", "cover.webp", "image/jpeg", "data".getBytes()
            );

            when(imageProcessingService.compressImage(webp, maxWidth)).thenReturn(compressed);
            setupSuccessfulUpload();

            s3Service.uploadFile(webp, folder, fileName);
            verify(imageProcessingService).compressImage(webp, maxWidth);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ERROR HANDLING
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tratamento de Erros AWS")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Deve lançar AmazonClientException em AwsServiceException")
        void shouldThrowAmazonClientExceptionOnAwsError() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", createValidImageBytes()
            );
            MockMultipartFile compressed = new MockMultipartFile(
                "file", "cover.png", "image/jpeg", "data".getBytes()
            );

            when(imageProcessingService.compressImage(file, maxWidth)).thenReturn(compressed);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(AwsServiceException.builder().message("Invalid credentials").build());

            assertThatThrownBy(() -> s3Service.uploadFile(file, folder, fileName))
                .isInstanceOf(AmazonClientException.class);
        }

        @Test
        @DisplayName("Deve lançar AmazonClientException em SdkClientException")
        void shouldThrowAmazonClientExceptionOnSdkError() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", createValidImageBytes()
            );
            MockMultipartFile compressed = new MockMultipartFile(
                "file", "cover.png", "image/jpeg", "data".getBytes()
            );

            when(imageProcessingService.compressImage(file, maxWidth)).thenReturn(compressed);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.builder().message("Timeout").build());

            assertThatThrownBy(() -> s3Service.uploadFile(file, folder, fileName))
                .isInstanceOf(AmazonClientException.class);
        }

        @Test
        @DisplayName("Deve lançar AmazonClientException em IOException (compressão)")
        void shouldThrowAmazonClientExceptionOnIOException() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", createValidImageBytes()
            );

            when(imageProcessingService.compressImage(file, maxWidth))
                .thenThrow(new IOException("Cannot read image"));

            assertThatThrownBy(() -> s3Service.uploadFile(file, folder, fileName))
                .isInstanceOf(AmazonClientException.class);
        }

        @Test
        @DisplayName("Deve lançar URIException em URISyntaxException")
        void shouldThrowURIExceptionOnInvalidUri() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", createValidImageBytes()
            );
            MockMultipartFile compressed = new MockMultipartFile(
                "file", "cover.png", "image/jpeg", "data".getBytes()
            );

            when(imageProcessingService.compressImage(file, maxWidth)).thenReturn(compressed);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
            when(s3Client.utilities()).thenReturn(s3Utilities);
            when(s3Utilities.getUrl(any(GetUrlRequest.class)))
            	.thenThrow(IllegalArgumentException.class); 
            
            assertThatThrownBy(() -> s3Service.uploadFile(file, folder, fileName))
                .isInstanceOf(URIException.class);
        }
    }
}
