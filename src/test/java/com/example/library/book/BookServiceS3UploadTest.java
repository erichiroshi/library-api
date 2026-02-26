package com.example.library.book;

import java.net.URI;
import java.util.Optional;

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
import org.springframework.web.multipart.MultipartFile;

import com.example.library.aws.S3Service;
import com.example.library.aws.exception.AmazonClientException;
import com.example.library.book.exception.BookNotFoundException;
import com.example.library.category.Category;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes para a funcionalidade de upload de imagem de capa do livro com S3
 * 
 * VERSÃO ATUALIZADA: Inclui mock do ImageProcessingService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService - Upload S3 Tests (Updated)")
class BookServiceS3UploadTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private S3Service s3Service;

    private BookService bookService;

    private Book book;
    private Category category;
    private MockMultipartFile validImage;
    private URI expectedS3Uri;

    @BeforeEach
    void setUp() {
        // Inicializar BookService
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        
        bookService = new BookService(
            bookRepository,
            null, // authorRepository
            null, // categoryRepository
            null, // bookMapper
            s3Service,
            meterRegistry,
            null  // delayService
        );
        
        // Inject prefix via ReflectionTestUtils (config mudou para estrutura aninhada)
        ReflectionTestUtils.setField(bookService, "prefix", "book-");

        // Setup category
        category = new Category();
        category.setId(1L);
        category.setName("Technology");

        // Setup book
        book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setIsbn("978-0132350884");
        book.setCategory(category);

        // Setup valid image file
        validImage = new MockMultipartFile(
            "file",
            "cover.png",
            "image/png",
            "fake-image-content".getBytes()
        );

        // Expected S3 URI
        expectedS3Uri = URI.create("https://library-api-s3.s3.sa-east-1.amazonaws.com/books/book-1.jpg");
    }

    // ═══════════════════════════════════════════════════════════════════
    // UPLOAD FILE - SUCCESS CASES
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadFile() - Casos de Sucesso")
    class UploadFileSuccessTests {

        @Test
        @DisplayName("Deve fazer upload de imagem PNG com sucesso")
        void shouldUploadPngImageSuccessfully() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            // Act
            URI result = bookService.uploadFile(1L, validImage);

            // Assert
            assertThat(result).isEqualTo(expectedS3Uri);

            // Verifica que o S3Service foi chamado corretamente
            verify(s3Service).uploadFile(validImage, "books/", "book-1");

            // Verifica que o livro foi atualizado com a URL
            ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(bookCaptor.capture());
            
            Book savedBook = bookCaptor.getValue();
            assertThat(savedBook.getCoverImageUrl()).isEqualTo(expectedS3Uri.toString());
            assertThat(savedBook.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Deve fazer upload de imagem JPEG com sucesso")
        void shouldUploadJpegImageSuccessfully() {
            // Arrange
            MockMultipartFile jpegImage = new MockMultipartFile(
                "file",
                "cover.jpg",
                "image/jpeg",
                "fake-jpeg-content".getBytes()
            );

            URI jpegUri = URI.create("https://library-api-s3.s3.sa-east-1.amazonaws.com/books/book-1.jpg");

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(jpegUri);

            // Act
            URI result = bookService.uploadFile(1L, jpegImage);

            // Assert
            assertThat(result).isEqualTo(jpegUri);
            verify(s3Service).uploadFile(jpegImage, "books/", "book-1");
        }

        @Test
        @DisplayName("Deve substituir URL existente ao fazer novo upload")
        void shouldReplaceExistingUrlOnNewUpload() {
            // Arrange
            book.setCoverImageUrl("https://old-url.com/old-image.png");
            
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            // Act
            bookService.uploadFile(1L, validImage);

            // Assert
            ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(bookCaptor.capture());
            
            Book savedBook = bookCaptor.getValue();
            assertThat(savedBook.getCoverImageUrl()).isEqualTo(expectedS3Uri.toString());
            assertThat(savedBook.getCoverImageUrl()).isNotEqualTo("https://old-url.com/old-image.png");
        }

        @Test
        @DisplayName("Deve usar prefixo correto 'book-' no nome do arquivo")
        void shouldUseCorrectFilenamePrefix() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            // Act
            bookService.uploadFile(1L, validImage);

            // Assert
            // Verifica que o prefixo "book-" + ID está correto
            verify(s3Service).uploadFile(validImage, "books/", "book-1");
        }

        @Test
        @DisplayName("Deve usar pasta correta 'books/' no S3")
        void shouldUseCorrectS3Folder() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            // Act
            bookService.uploadFile(1L, validImage);

            // Assert
            verify(s3Service).uploadFile(validImage, "books/", "book-1");
        }

        @Test
        @DisplayName("Deve salvar livro com URL após upload bem-sucedido")
        void shouldSaveBookWithUrlAfterSuccessfulUpload() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            // Act
            bookService.uploadFile(1L, validImage);

            // Assert
            verify(bookRepository).save(book);
            assertThat(book.getCoverImageUrl()).isEqualTo(expectedS3Uri.toString());
        }

        @Test
        @DisplayName("Deve usar ID do livro existente no nome do arquivo")
        void shouldUseExistingBookIdInFilename() {
            // Arrange
            Book bookWithLargeId = new Book();
            bookWithLargeId.setId(12345L);
            bookWithLargeId.setTitle("Large ID Book");
            bookWithLargeId.setCategory(category);

            when(bookRepository.findById(12345L)).thenReturn(Optional.of(bookWithLargeId));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-12345")))
                .thenReturn(expectedS3Uri);

            // Act
            bookService.uploadFile(12345L, validImage);

            // Assert
            verify(s3Service).uploadFile(validImage, "books/", "book-12345");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // UPLOAD FILE - ERROR CASES
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadFile() - Casos de Erro")
    class UploadFileErrorTests {

        @Test
        @DisplayName("Deve lançar BookNotFoundException quando livro não existe")
        void shouldThrowBookNotFoundExceptionWhenBookDoesNotExist() {
            // Arrange
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookService.uploadFile(999L, validImage))
                .isInstanceOf(BookNotFoundException.class);

            // Verifica que S3Service não foi chamado
            verify(s3Service, never()).uploadFile(any(), any(), any());
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve propagar AmazonClientException quando upload falha no S3")
        void shouldPropagateAmazonClientExceptionOnS3Failure() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenThrow(new AmazonClientException());

            // Act & Assert
            assertThatThrownBy(() -> bookService.uploadFile(1L, validImage))
                .isInstanceOf(AmazonClientException.class);

            // Verifica que o livro NÃO foi salvo
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Não deve salvar livro quando S3Service lança exceção")
        void shouldNotSaveBookWhenS3ServiceThrowsException() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenThrow(new RuntimeException("S3 connection failed"));

            // Act & Assert
            assertThatThrownBy(() -> bookService.uploadFile(1L, validImage))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 connection failed");

            // Verifica que o repository.save() não foi chamado
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando S3 valida content type inválido")
        void shouldThrowIllegalArgumentExceptionWhenInvalidContentType() {
            // Arrange
            MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",  // ❌ Não é imagem
                "fake-pdf-content".getBytes()
            );

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenThrow(new IllegalArgumentException("Invalid content type: application/pdf"));

            // Act & Assert
            assertThatThrownBy(() -> bookService.uploadFile(1L, invalidFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid content type");

            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando arquivo é muito grande")
        void shouldThrowIllegalArgumentExceptionWhenFileTooLarge() {
            // Arrange
            byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB (acima do limite)
            MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                largeContent
            );

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenThrow(new IllegalArgumentException("File too large"));

            // Act & Assert
            assertThatThrownBy(() -> bookService.uploadFile(1L, largeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File too large");

            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando arquivo é muito pequeno")
        void shouldThrowIllegalArgumentExceptionWhenFileTooSmall() {
            // Arrange
            MockMultipartFile tinyFile = new MockMultipartFile(
                "file",
                "tiny.png",
                "image/png",
                new byte[100]  // 100 bytes (< 1KB)
            );

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenThrow(new IllegalArgumentException("File too small"));

            // Act & Assert
            assertThatThrownBy(() -> bookService.uploadFile(1L, tinyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File too small");

            verify(bookRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // EDGE CASES
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadFile() - Casos Extremos")
    class UploadFileEdgeCaseTests {

        @Test
        @DisplayName("Deve lidar com ID de livro muito grande")
        void shouldHandleVeryLargeBookId() {
            // Arrange
            Long largeId = Long.MAX_VALUE;
            Book bookWithLargeId = new Book();
            bookWithLargeId.setId(largeId);
            bookWithLargeId.setCategory(category);

            when(bookRepository.findById(largeId)).thenReturn(Optional.of(bookWithLargeId));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-" + largeId)))
                .thenReturn(expectedS3Uri);

            // Act
            bookService.uploadFile(largeId, validImage);

            // Assert
            verify(s3Service).uploadFile(validImage, "books/", "book-" + largeId);
        }

        @Test
        @DisplayName("Deve lidar com diferentes tipos de imagem permitidos")
        void shouldHandleDifferentAllowedImageTypes() {
            // Arrange
            MockMultipartFile webpImage = new MockMultipartFile(
                "file",
                "cover.webp",
                "image/webp",
                "fake-webp-content".getBytes()
            );

            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            // Act
            bookService.uploadFile(1L, webpImage);

            // Assert
            verify(s3Service).uploadFile(webpImage, "books/", "book-1");
        }

        @Test
        @DisplayName("Deve processar múltiplos uploads para o mesmo livro")
        void shouldHandleMultipleUploadsForSameBook() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            
            URI firstUri = URI.create("https://s3.amazonaws.com/books/book-1-v1.jpg");
            URI secondUri = URI.create("https://s3.amazonaws.com/books/book-1-v2.jpg");
            
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(firstUri)
                .thenReturn(secondUri);

            // Act - Primeiro upload
            URI result1 = bookService.uploadFile(1L, validImage);
            
            // Assert - Primeiro upload
            assertThat(result1).isEqualTo(firstUri);
            assertThat(book.getCoverImageUrl()).isEqualTo(firstUri.toString());

            // Act - Segundo upload
            URI result2 = bookService.uploadFile(1L, validImage);
            
            // Assert - Segundo upload substitui
            assertThat(result2).isEqualTo(secondUri);
            assertThat(book.getCoverImageUrl()).isEqualTo(secondUri.toString());
            
            // Verifica que save foi chamado 2 vezes
            verify(bookRepository, org.mockito.Mockito.times(2)).save(book);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // INTEGRATION BEHAVIOR
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadFile() - Comportamento de Integração")
    class UploadFileIntegrationBehaviorTests {

        @Test
        @DisplayName("Deve buscar livro antes de fazer upload")
        void shouldFetchBookBeforeUpload() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            // Act
            bookService.uploadFile(1L, validImage);

            // Assert
            // Verifica ordem de chamadas
            var inOrder = org.mockito.Mockito.inOrder(bookRepository, s3Service);
            inOrder.verify(bookRepository).findById(1L);
            inOrder.verify(s3Service).uploadFile(any(), any(), any());
            inOrder.verify(bookRepository).save(any());
        }

        @Test
        @DisplayName("Deve retornar a URI retornada pelo S3Service")
        void shouldReturnUriFromS3Service() {
            // Arrange
            URI customUri = URI.create("https://custom-bucket.s3.amazonaws.com/books/book-1.jpg");
            
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(customUri);

            // Act
            URI result = bookService.uploadFile(1L, validImage);

            // Assert
            assertThat(result).isEqualTo(customUri);
        }

        @Test
        @DisplayName("Deve salvar livro com a mesma URI retornada")
        void shouldSaveBookWithSameUriReturned() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            // Act
            URI returnedUri = bookService.uploadFile(1L, validImage);

            // Assert
            ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(bookCaptor.capture());
            
            Book savedBook = bookCaptor.getValue();
            assertThat(savedBook.getCoverImageUrl()).isEqualTo(returnedUri.toString());
        }

        @Test
        @DisplayName("Deve manter outros campos do livro inalterados")
        void shouldKeepOtherBookFieldsUnchanged() {
            // Arrange
            book.setTitle("Original Title");
            book.setIsbn("978-0132350884");
            book.setPublicationYear(2008);
            
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(s3Service.uploadFile(any(MultipartFile.class), eq("books/"), eq("book-1")))
                .thenReturn(expectedS3Uri);

            // Act
            bookService.uploadFile(1L, validImage);

            // Assert
            ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(bookCaptor.capture());
            
            Book savedBook = bookCaptor.getValue();
            assertThat(savedBook.getTitle()).isEqualTo("Original Title");
            assertThat(savedBook.getIsbn()).isEqualTo("978-0132350884");
            assertThat(savedBook.getPublicationYear()).isEqualTo(2008);
            // Apenas coverImageUrl foi modificado
            assertThat(savedBook.getCoverImageUrl()).isEqualTo(expectedS3Uri.toString());
        }
    }
}