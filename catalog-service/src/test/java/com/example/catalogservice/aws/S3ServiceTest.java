package com.example.catalogservice.aws;

import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import com.example.catalogservice.aws.exception.AmazonClientException;
import com.example.catalogservice.aws.exception.S3UnavailableException;
import com.example.catalogservice.aws.utils.ImageProcessingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// ═══════════════════════════════════════════════════════════════════════════════
// S3ServiceTest
// ═══════════════════════════════════════════════════════════════════════════════

@ExtendWith(MockitoExtension.class)
@DisplayName("S3Service")
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private ImageProcessingService imageProcessingService;

    @InjectMocks
    private S3Service s3Service;

    private static final String BUCKET = "library-api-s3";
    private static final String COVER_URL =
            "https://library-api-s3.s3.sa-east-1.amazonaws.com/books/book-1.jpg";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3Service, "bucketName", BUCKET);
        ReflectionTestUtils.setField(s3Service, "maxWidth", 400);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // uploadFile
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("uploadFile()")
    class UploadFile {

        private MockMultipartFile pngFile;

        @BeforeEach
        void setUp() {
            pngFile = new MockMultipartFile(
                    "file", "cover.png", "image/png",
                    new byte[2048]); // 2KB — dentro do limite
        }

        @Test
        @DisplayName("Deve fazer upload de PNG e retornar URI do S3")
        void shouldUploadPngAndReturnUri() throws Exception {
            MockMultipartFile compressed = new MockMultipartFile(
                    "file", "cover.png", "image/png", new byte[1500]);
            when(imageProcessingService.compressImage(pngFile, 400)).thenReturn(compressed);

            S3Utilities utilities = mockS3UtilitiesWithUrl(
                    "https://" + BUCKET + ".s3.sa-east-1.amazonaws.com/books/book-1.png");
            when(s3Client.utilities()).thenReturn(utilities);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            URI result = s3Service.uploadFile(pngFile, "books/", "book-1");

            assertThat(result).isNotNull();
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("Deve usar bucket e key corretos no PutObjectRequest")
        void shouldUseBucketAndKeyInPutRequest() throws Exception {
            MockMultipartFile compressed = new MockMultipartFile(
                    "file", "cover.png", "image/png", new byte[1500]);
            when(imageProcessingService.compressImage(pngFile, 400)).thenReturn(compressed);

            S3Utilities utilities = mockS3UtilitiesWithUrl(
                    "https://" + BUCKET + ".s3.sa-east-1.amazonaws.com/books/book-1.png");
            when(s3Client.utilities()).thenReturn(utilities);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            s3Service.uploadFile(pngFile, "books/", "book-1");

            ArgumentCaptor<PutObjectRequest> captor =
                    ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

            PutObjectRequest request = captor.getValue();
            assertThat(request.bucket()).isEqualTo(BUCKET);
            assertThat(request.key()).isEqualTo("books/book-1.png");
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException para arquivo menor que 1KB")
        void shouldThrowWhenFileTooSmall() {
            MockMultipartFile tinyFile = new MockMultipartFile(
                    "file", "cover.png", "image/png", new byte[100]);

            assertThatThrownBy(() -> s3Service.uploadFile(tinyFile, "books/", "book-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File too small");

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException para arquivo maior que 10MB")
        void shouldThrowWhenFileTooLarge() {
            MockMultipartFile largeFile = new MockMultipartFile(
                    "file", "cover.png", "image/png",
                    new byte[11 * 1024 * 1024]);

            assertThatThrownBy(() -> s3Service.uploadFile(largeFile, "books/", "book-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File too large");

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException para content-type inválido")
        void shouldThrowForInvalidContentType() {
            MockMultipartFile pdfFile = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", new byte[2048]);

            assertThatThrownBy(() -> s3Service.uploadFile(pdfFile, "books/", "book-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid content type");

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException para content-type null")
        void shouldThrowForNullContentType() {
            MockMultipartFile nullTypeFile = new MockMultipartFile(
                    "file", "cover.png", null, new byte[2048]);

            assertThatThrownBy(() -> s3Service.uploadFile(nullTypeFile, "books/", "book-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid content type");
        }

        @Test
        @DisplayName("Deve lançar AmazonClientException quando S3 lança AwsServiceException")
        void shouldThrowAmazonClientExceptionOnAwsError() throws Exception {
            MockMultipartFile compressed = new MockMultipartFile(
                    "file", "cover.png", "image/png", new byte[1500]);
            when(imageProcessingService.compressImage(pngFile, 400)).thenReturn(compressed);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(AwsServiceException.builder().message("S3 error").build());

            assertThatThrownBy(() -> s3Service.uploadFile(pngFile, "books/", "book-1"))
                    .isInstanceOf(AmazonClientException.class);
        }

        @Test
        @DisplayName("Deve lançar AmazonClientException quando S3 lança SdkClientException")
        void shouldThrowAmazonClientExceptionOnSdkError() throws Exception {
            MockMultipartFile compressed = new MockMultipartFile(
                    "file", "cover.png", "image/png", new byte[1500]);
            when(imageProcessingService.compressImage(pngFile, 400)).thenReturn(compressed);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenThrow(SdkClientException.builder().message("SDK error").build());

            assertThatThrownBy(() -> s3Service.uploadFile(pngFile, "books/", "book-1"))
                    .isInstanceOf(AmazonClientException.class);
        }

        @Test
        @DisplayName("Deve aceitar image/jpeg e mapear extensão para jpg")
        void shouldAcceptJpegAndMapExtensionToJpg() throws Exception {
            MockMultipartFile jpegFile = new MockMultipartFile(
                    "file", "cover.jpg", "image/jpeg", new byte[2048]);
            MockMultipartFile compressed = new MockMultipartFile(
                    "file", "cover.jpg", "image/jpeg", new byte[1500]);
            when(imageProcessingService.compressImage(jpegFile, 400)).thenReturn(compressed);

            S3Utilities utilities = mockS3UtilitiesWithUrl(
                    "https://" + BUCKET + ".s3.sa-east-1.amazonaws.com/books/book-1.jpg");
            when(s3Client.utilities()).thenReturn(utilities);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            s3Service.uploadFile(jpegFile, "books/", "book-1");

            ArgumentCaptor<PutObjectRequest> captor =
                    ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
            assertThat(captor.getValue().key()).isEqualTo("books/book-1.jpg");
        }

        @Test
        @DisplayName("Deve aceitar image/webp e mapear extensão para webp")
        void shouldAcceptWebpAndMapExtensionToWebp() throws Exception {
            MockMultipartFile webpFile = new MockMultipartFile(
                    "file", "cover.webp", "image/webp", new byte[2048]);
            MockMultipartFile compressed = new MockMultipartFile(
                    "file", "cover.webp", "image/webp", new byte[1500]);
            when(imageProcessingService.compressImage(webpFile, 400)).thenReturn(compressed);

            S3Utilities utilities = mockS3UtilitiesWithUrl(
                    "https://" + BUCKET + ".s3.sa-east-1.amazonaws.com/books/book-1.webp");
            when(s3Client.utilities()).thenReturn(utilities);
            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            s3Service.uploadFile(webpFile, "books/", "book-1");

            ArgumentCaptor<PutObjectRequest> captor =
                    ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
            assertThat(captor.getValue().key()).isEqualTo("books/book-1.webp");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteCover
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteCover()")
    class DeleteCover {
     
        @Test
        @DisplayName("Deve deletar objeto S3 usando a key extraída da URL")
        void shouldDeleteObjectUsingKeyFromUrl() {
            var captured = stubDeleteObject();
     
            s3Service.deleteCover(COVER_URL);
     
            assertThat(captured.get().bucket()).isEqualTo(BUCKET);
            assertThat(captured.get().key()).isEqualTo("books/book-1.jpg");
        }
     
        @Test
        @DisplayName("Deve extrair key corretamente removendo o leading slash da URL")
        void shouldExtractKeyWithoutLeadingSlash() {
            var captured = stubDeleteObject();
     
            s3Service.deleteCover(COVER_URL);
     
            assertThat(captured.get().key())
                    .doesNotStartWith("/")
                    .isEqualTo("books/book-1.jpg");
        }
     
        @Test
        @DisplayName("Deve ignorar silenciosamente quando URL é null")
        void shouldSkipWhenUrlIsNull() {
            assertThatCode(() -> s3Service.deleteCover(null))
                    .doesNotThrowAnyException();
     
            verify(s3Client, never()).deleteObject(
                    org.mockito.ArgumentMatchers
                            .<java.util.function.Consumer<
                                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>>
                            any());
        }
     
        @Test
        @DisplayName("Deve ignorar silenciosamente quando URL é blank")
        void shouldSkipWhenUrlIsBlank() {
            assertThatCode(() -> s3Service.deleteCover("   "))
                    .doesNotThrowAnyException();
     
            verify(s3Client, never()).deleteObject(
                    org.mockito.ArgumentMatchers
                            .<java.util.function.Consumer<
                                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>>
                            any());
        }
     
        @Test
        @DisplayName("Deve lançar AmazonClientException quando S3 lança AwsServiceException")
        void shouldThrowAmazonClientExceptionOnAwsError() {
            doThrow(AwsServiceException.builder().message("S3 error").build())
                    .when(s3Client).deleteObject(
                            org.mockito.ArgumentMatchers
                                    .<java.util.function.Consumer<
                                            software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>>
                                    any());
     
            assertThatThrownBy(() -> s3Service.deleteCover(COVER_URL))
                    .isInstanceOf(AmazonClientException.class);
        }
     
        @Test
        @DisplayName("Deve lançar AmazonClientException quando S3 lança SdkClientException")
        void shouldThrowAmazonClientExceptionOnSdkError() {
            doThrow(SdkClientException.builder().message("SDK error").build())
                    .when(s3Client).deleteObject(
                            org.mockito.ArgumentMatchers
                                    .<java.util.function.Consumer<
                                            software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>>
                                    any());
     
            assertThatThrownBy(() -> s3Service.deleteCover(COVER_URL))
                    .isInstanceOf(AmazonClientException.class);
        }
     
        @Test
        @DisplayName("Deve funcionar com diferentes paths no S3")
        void shouldHandleDifferentS3Paths() {
            var captured = stubDeleteObject();
     
            s3Service.deleteCover(
                    "https://library-api-s3.s3.sa-east-1.amazonaws.com/images/covers/book-42.webp");
     
            assertThat(captured.get().key()).isEqualTo("images/covers/book-42.webp");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteFallback (comportamento silencioso)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteFallback()")
    class DeleteFallback {

        @Test
        @DisplayName("Não deve lançar exceção quando deleteFallback é invocado")
        void shouldNotThrowExceptionInDeleteFallback() throws Exception {
            // Invoca o fallback diretamente via reflection para garantir o contrato
            var method = S3Service.class.getDeclaredMethod(
                    "deleteFallback", String.class, Exception.class);
            method.setAccessible(true);

            assertThatCode(() ->
                    method.invoke(s3Service, COVER_URL, new RuntimeException("S3 down")))
                    .doesNotThrowAnyException();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // uploadFallback (lança S3UnavailableException)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("uploadFallback()")
    class UploadFallback {

        @Test
        @DisplayName("Deve lançar S3UnavailableException quando uploadFallback é invocado")
        void shouldThrowS3UnavailableExceptionInUploadFallback() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cover.png", "image/png", new byte[2048]);

            var method = S3Service.class.getDeclaredMethod(
                    "uploadFallback", MultipartFile.class, String.class, String.class, Exception.class);
            method.setAccessible(true);

            assertThatThrownBy(() -> {
                try {
                    method.invoke(s3Service, file, "books/", "book-1",
                            new RuntimeException("Circuit open"));
                } catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            }).isInstanceOf(S3UnavailableException.class)
              .hasMessageContaining("temporarily unavailable");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // helper
    // ─────────────────────────────────────────────────────────────────────────

    private S3Utilities mockS3UtilitiesWithUrl(String urlString) throws Exception {
        S3Utilities utilities = org.mockito.Mockito.mock(S3Utilities.class);
        URL url = URI.create(urlString).toURL();
        when(utilities.getUrl(any(GetUrlRequest.class))).thenReturn(url);
        return utilities;
    }
    
 // ── Helper a adicionar na classe S3ServiceTest ────────────────────────────────
    
    /**
     * AWS SDK v2 usa Consumer<Builder> no deleteObject — não aceita DeleteObjectRequest direto.
     * Este helper captura o request construído pelo Consumer para inspeção nos testes.
     */
    private java.util.concurrent.atomic.AtomicReference<
            software.amazon.awssdk.services.s3.model.DeleteObjectRequest>
    stubDeleteObject() {
        var captured = new java.util.concurrent.atomic.AtomicReference<
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest>();
     
        doAnswer(invocation -> {
            java.util.function.Consumer<
                    software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>
                    consumer = invocation.getArgument(0);
            var builder = software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder();
            consumer.accept(builder);
            captured.set(builder.build());
            return software.amazon.awssdk.services.s3.model.DeleteObjectResponse.builder().build();
        }).when(s3Client).deleteObject(
                org.mockito.ArgumentMatchers
                        .<java.util.function.Consumer<
                                software.amazon.awssdk.services.s3.model.DeleteObjectRequest.Builder>>
                        any());
     
        return captured;
    }	
}

