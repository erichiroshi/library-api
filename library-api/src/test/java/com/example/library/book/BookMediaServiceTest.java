package com.example.library.book;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.library.aws.S3Service;
import com.example.library.aws.exception.AmazonClientException;
import com.example.library.book.exception.BookNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookMediaService - Upload S3 Tests")
class BookMediaServiceTest {

    @Mock
    private BookService bookService;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private BookMediaService mediaService;

    private Book book;
    private MockMultipartFile validImage;
    private URI expectedS3Uri;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaService, "prefix", "book-");

        book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setIsbn("978-0132350884");
        book.setPublicationYear(2008);

        validImage = new MockMultipartFile(
            "file",
            "cover.png",
            "image/png",
            "fake-image-content".getBytes()
        );

        expectedS3Uri = URI.create(
            "https://library-api-s3.s3.sa-east-1.amazonaws.com/books/book-1.jpg"
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // SUCCESS CASES
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadCover() - Casos de Sucesso")
    class UploadCoverSuccessTests {

        @Test
        @DisplayName("Deve fazer upload de imagem PNG e retornar URI")
        void shouldUploadPngImageAndReturnUri() {
            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            URI result = mediaService.uploadCover(1L, validImage);

            assertThat(result).isEqualTo(expectedS3Uri);
        }

        @Test
        @DisplayName("Deve fazer upload de imagem JPEG e retornar URI")
        void shouldUploadJpegImageAndReturnUri() {
            MockMultipartFile jpegImage = new MockMultipartFile(
                "file", "cover.jpg", "image/jpeg", "fake-jpeg-content".getBytes()
            );
            URI jpegUri = URI.create(
                "https://library-api-s3.s3.sa-east-1.amazonaws.com/books/book-1.jpg"
            );

            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(jpegUri);

            URI result = mediaService.uploadCover(1L, jpegImage);

            assertThat(result).isEqualTo(jpegUri);
        }

        @Test
        @DisplayName("Deve fazer upload de imagem WEBP e retornar URI")
        void shouldUploadWebpImageAndReturnUri() {
            MockMultipartFile webpImage = new MockMultipartFile(
                "file", "cover.webp", "image/webp", "fake-webp-content".getBytes()
            );

            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            URI result = mediaService.uploadCover(1L, webpImage);

            assertThat(result).isEqualTo(expectedS3Uri);
        }

        @Test
        @DisplayName("Deve usar prefixo 'book-' concatenado com o ID no nome do arquivo")
        void shouldUseCorrectFilenameWithPrefixAndId() {
            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            mediaService.uploadCover(1L, validImage);

            verify(s3Service).uploadFile(validImage, "books/", "book-1");
        }

        @Test
        @DisplayName("Deve usar pasta 'books/' como folder no S3")
        void shouldUseCorrectS3Folder() {
            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            mediaService.uploadCover(1L, validImage);

            verify(s3Service).uploadFile(validImage, "books/", "book-1");
        }

        @Test
        @DisplayName("Deve chamar updateCoverImageUrl com fileName correto após upload")
        void shouldCallUpdateCoverImageUrlWithCorrectFileName() {
            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            mediaService.uploadCover(1L, validImage);

            verify(bookService).updateCoverImageUrl(1L, "book-1");
        }

        @Test
        @DisplayName("Deve usar ID de livro grande corretamente no nome do arquivo")
        void shouldHandleLargeBookIdInFilename() {
            Book bookLargeId = new Book();
            bookLargeId.setId(12345L);

            when(bookService.find(12345L)).thenReturn(bookLargeId);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-12345")))
                .thenReturn(expectedS3Uri);

            mediaService.uploadCover(12345L, validImage);

            verify(s3Service).uploadFile(validImage, "books/", "book-12345");
            verify(bookService).updateCoverImageUrl(12345L, "book-12345");
        }

        @Test
        @DisplayName("Deve usar Long.MAX_VALUE corretamente no nome do arquivo")
        void shouldHandleMaxLongBookId() {
            Long maxId = Long.MAX_VALUE;
            Book bookMaxId = new Book();
            bookMaxId.setId(maxId);

            when(bookService.find(maxId)).thenReturn(bookMaxId);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-" + maxId)))
                .thenReturn(expectedS3Uri);

            mediaService.uploadCover(maxId, validImage);

            verify(s3Service).uploadFile(validImage, "books/", "book-" + maxId);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ERROR CASES
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadCover() - Casos de Erro")
    class UploadCoverErrorTests {

        @Test
        @DisplayName("Deve retornar null quando livro não existe (BookNotFoundException capturada)")
        void shouldReturnNullWhenBookNotFound() {
            when(bookService.find(999L)).thenThrow(new BookNotFoundException(999L));

            URI result = mediaService.uploadCover(999L, validImage);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Não deve chamar S3Service quando livro não existe")
        void shouldNotCallS3WhenBookNotFound() {
            when(bookService.find(999L)).thenThrow(new BookNotFoundException(999L));

            mediaService.uploadCover(999L, validImage);

            verify(s3Service, never()).uploadFile(any(), any(), any());
        }

        @Test
        @DisplayName("Não deve chamar updateCoverImageUrl quando livro não existe")
        void shouldNotUpdateCoverWhenBookNotFound() {
            when(bookService.find(999L)).thenThrow(new BookNotFoundException(999L));

            mediaService.uploadCover(999L, validImage);

            verify(bookService, never()).updateCoverImageUrl(anyLong(), anyString());
        }

        @Test
        @DisplayName("Deve propagar exceção quando S3Service falha")
        void shouldPropagateExceptionWhenS3Fails() {
            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenThrow(new AmazonClientException());

            assertThatThrownBy(() -> mediaService.uploadCover(1L, validImage))
                .isInstanceOf(AmazonClientException.class);
        }

        @Test
        @DisplayName("Não deve chamar updateCoverImageUrl quando S3Service lança exceção")
        void shouldNotUpdateCoverWhenS3Fails() {
            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenThrow(new RuntimeException("S3 error"));

            assertThatThrownBy(() -> mediaService.uploadCover(1L, validImage))
                .isInstanceOf(RuntimeException.class);

            verify(bookService, never()).updateCoverImageUrl(anyLong(), anyString());
        }

        @Test
        @DisplayName("Deve propagar IllegalArgumentException quando S3 valida content-type inválido")
        void shouldPropagateExceptionOnInvalidContentType() {
            MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "fake-pdf".getBytes()
            );

            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenThrow(new IllegalArgumentException("Invalid content type: application/pdf"));

            assertThatThrownBy(() -> mediaService.uploadCover(1L, pdfFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid content type");

            verify(bookService, never()).updateCoverImageUrl(anyLong(), anyString());
        }

        @Test
        @DisplayName("Deve propagar IllegalArgumentException quando arquivo é muito grande")
        void shouldPropagateExceptionWhenFileTooLarge() {
            byte[] largeContent = new byte[11 * 1024 * 1024];
            MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.png", "image/png", largeContent
            );

            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenThrow(new IllegalArgumentException("File too large"));

            assertThatThrownBy(() -> mediaService.uploadCover(1L, largeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File too large");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // INTERACTION ORDER
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadCover() - Ordem de Interações")
    class UploadCoverInteractionTests {

        @Test
        @DisplayName("Deve buscar livro, fazer upload e atualizar URL nessa ordem")
        void shouldExecuteInCorrectOrder() {
            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            mediaService.uploadCover(1L, validImage);

            var inOrder = inOrder(bookService, s3Service);
            inOrder.verify(bookService).find(1L);
            inOrder.verify(s3Service).uploadFile(any(), any(), any());
            inOrder.verify(bookService).updateCoverImageUrl(1L, "book-1");
        }

        @Test
        @DisplayName("Deve processar múltiplos uploads para o mesmo livro")
        void shouldHandleMultipleUploadsForSameBook() {
            URI firstUri = URI.create("https://s3.amazonaws.com/books/book-1-v1.jpg");
            URI secondUri = URI.create("https://s3.amazonaws.com/books/book-1-v2.jpg");

            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenReturn(firstUri)
                .thenReturn(secondUri);

            URI result1 = mediaService.uploadCover(1L, validImage);
            URI result2 = mediaService.uploadCover(1L, validImage);

            assertThat(result1).isEqualTo(firstUri);
            assertThat(result2).isEqualTo(secondUri);
            verify(bookService, times(2)).updateCoverImageUrl(1L, "book-1");
        }

        @Test
        @DisplayName("Deve retornar a URI exata retornada pelo S3Service")
        void shouldReturnExactUriFromS3Service() {
            URI customUri = URI.create(
                "https://custom-bucket.s3.us-east-1.amazonaws.com/books/book-1.webp"
            );

            when(bookService.find(1L)).thenReturn(book);
            when(s3Service.uploadFile(any(), eq("books/"), eq("book-1")))
                .thenReturn(customUri);

            URI result = mediaService.uploadCover(1L, validImage);

            assertThat(result).isEqualTo(customUri);
        }
    }
}