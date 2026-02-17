package com.example.library.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.library.author.Author;
import com.example.library.author.AuthorRepository;
import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.book.BookService;
import com.example.library.book.dto.BookCreateDTO;
import com.example.library.book.dto.BookResponseDTO;
import com.example.library.book.dto.PageResponseDTO;
import com.example.library.book.exception.BookAlreadyExistsException;
import com.example.library.book.exception.BookNotFoundException;
import com.example.library.book.exception.InvalidOperationException;
import com.example.library.book.mapper.BookMapper;
import com.example.library.category.Category;
import com.example.library.category.CategoryRepository;
import com.example.library.category.exception.CategoryNotFoundException;
import com.example.library.shared.config.delaycachetest.ArtificialDelayService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService - Unit Tests")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private ArtificialDelayService delayService;

    private MeterRegistry meterRegistry;

    private BookService bookService;

    private Category category;
    private Author author;
    private Book book;
    private BookCreateDTO validDTO;
    private BookResponseDTO bookResponseDTO;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        bookService = new BookService(
            bookRepository,
            authorRepository,
            categoryRepository,
            bookMapper,
            meterRegistry,
            delayService
        );

        // Category
        category = new Category();
        category.setId(1L);
        category.setName("Technology");

        // Author
        author = new Author();
        author.setId(1L);
        author.setName("Robert C. Martin");

        // Book
        book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setIsbn("978-0132350884");
        book.setPublicationYear(2008);
        book.setAvailableCopies(5);
        book.setCategory(category);
        book.getAuthors().add(author);

        // Valid DTO
        validDTO = BookCreateDTO.builder()
            .title("Clean Code")
            .isbn("978-0132350884")
            .publicationYear(2008)
            .availableCopies(5)
            .authorIds(Set.of(1L))
            .categoryId(1L)
            .build();

        // Response DTO
        bookResponseDTO = new BookResponseDTO(
            1L,
            "Clean Code",
            "978-0132350884",
            2008,
            5,
            Set.of(),
            1L
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create() - criar livro")
    class CreateBookTests {

        @Test
        @DisplayName("Deve criar livro com sucesso")
        void shouldCreateBookSuccessfully() {
            // Arrange
            when(bookRepository.existsByIsbn(validDTO.isbn())).thenReturn(false);
            when(bookMapper.toEntity(validDTO)).thenReturn(book);
            when(authorRepository.findAllById(validDTO.authorIds())).thenReturn(List.of(author));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(bookRepository.save(any(Book.class))).thenReturn(book);
            when(bookMapper.toDTO(book)).thenReturn(bookResponseDTO);

            // Act
            BookResponseDTO result = bookService.create(validDTO);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("Clean Code");

            verify(bookRepository).existsByIsbn(validDTO.isbn());
            verify(bookRepository).save(any(Book.class));

            // Verifica incremento do counter
            Counter counter = meterRegistry.find("library.books.created").counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Deve lançar InvalidOperationException quando authorIds está vazio")
        void shouldThrowInvalidOperationWhenNoAuthors() {
            // Arrange
            BookCreateDTO dtoWithoutAuthors = BookCreateDTO.builder()
                .title("Book")
                .isbn("123456")
                .authorIds(Set.of())
                .categoryId(1L)
                .build();

            // Act & Assert
            assertThatThrownBy(() -> bookService.create(dtoWithoutAuthors))
                .isInstanceOf(InvalidOperationException.class);

            verifyNoInteractions(bookRepository);
            verifyNoInteractions(authorRepository);
            verifyNoInteractions(categoryRepository);
        }

        @Test
        @DisplayName("Deve lançar BookAlreadyExistsException quando ISBN já existe")
        void shouldThrowBookAlreadyExistsException() {
            // Arrange
            when(bookRepository.existsByIsbn(validDTO.isbn())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> bookService.create(validDTO))
                .isInstanceOf(BookAlreadyExistsException.class);

            verify(bookRepository).existsByIsbn(validDTO.isbn());
            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar CategoryNotFoundException quando categoria não existe")
        void shouldThrowCategoryNotFoundException() {
            // Arrange
            when(bookRepository.existsByIsbn(validDTO.isbn())).thenReturn(false);
            when(bookMapper.toEntity(validDTO)).thenReturn(book);
            when(authorRepository.findAllById(validDTO.authorIds())).thenReturn(List.of(author));
            when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookService.create(validDTO))
                .isInstanceOf(CategoryNotFoundException.class);

            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar InvalidOperationException quando autor não existe")
        void shouldThrowInvalidOperationWhenAuthorNotFound() {
            // Arrange
            when(bookRepository.existsByIsbn(validDTO.isbn())).thenReturn(false);
            when(bookMapper.toEntity(validDTO)).thenReturn(book);
            when(authorRepository.findAllById(validDTO.authorIds())).thenReturn(List.of()); // Nenhum autor encontrado
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            // Act & Assert
            assertThatThrownBy(() -> bookService.create(validDTO))
                .isInstanceOf(InvalidOperationException.class);

            verify(bookRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar livro com múltiplos autores")
        void shouldCreateBookWithMultipleAuthors() {
            // Arrange
            Author author2 = new Author();
            author2.setId(2L);
            author2.setName("Martin Fowler");

            BookCreateDTO dtoMultipleAuthors = BookCreateDTO.builder()
                .title("Refactoring")
                .isbn("978-0134757599")
                .publicationYear(2018)
                .availableCopies(3)
                .authorIds(Set.of(1L, 2L))
                .categoryId(1L)
                .build();

            when(bookRepository.existsByIsbn(dtoMultipleAuthors.isbn())).thenReturn(false);
            when(bookMapper.toEntity(dtoMultipleAuthors)).thenReturn(book);
            when(authorRepository.findAllById(dtoMultipleAuthors.authorIds()))
                .thenReturn(List.of(author, author2));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(bookRepository.save(any(Book.class))).thenReturn(book);

            // Act
            bookService.create(dtoMultipleAuthors);

            // Assert
            ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(bookCaptor.capture());
            assertThat(bookCaptor.getValue().getAuthors()).hasSize(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FIND BY ID
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById() - buscar por ID")
    class FindByIdTests {

        @Test
        @DisplayName("Deve retornar livro quando existe")
        void shouldReturnBookWhenExists() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
            when(bookMapper.toDTO(book)).thenReturn(bookResponseDTO);

            // Act
            BookResponseDTO result = bookService.findById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("Clean Code");

            verify(bookRepository).findById(1L);
            verify(delayService).delay(); // Verifica que o delay service foi chamado
        }

        @Test
        @DisplayName("Deve lançar BookNotFoundException quando não existe")
        void shouldThrowBookNotFoundException() {
            // Arrange
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookService.findById(999L))
                .isInstanceOf(BookNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FIND ALL
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAll() - buscar todos com paginação")
    class FindAllTests {

        @Test
        @DisplayName("Deve retornar página de livros")
        void shouldReturnPageOfBooks() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> page = new PageImpl<>(List.of(book), pageable, 1);

            when(bookRepository.findAll(pageable)).thenReturn(page);
            when(bookMapper.toDTO(book)).thenReturn(bookResponseDTO);

            // Act
            PageResponseDTO<BookResponseDTO> result = bookService.findAll(pageable);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.totalPages()).isEqualTo(1);

            verify(bookRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não há livros")
        void shouldReturnEmptyPageWhenNoBooks() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(bookRepository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            PageResponseDTO<BookResponseDTO> result = bookService.findAll(pageable);

            // Assert
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // DELETE
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteById() - deletar livro")
    class DeleteByIdTests {

        @Test
        @DisplayName("Deve deletar livro com sucesso")
        void shouldDeleteBookSuccessfully() {
            // Arrange
            when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

            // Act
            bookService.deleteById(1L);

            // Assert
            verify(bookRepository).findById(1L);
            verify(bookRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar BookNotFoundException ao tentar deletar livro inexistente")
        void shouldThrowBookNotFoundWhenDeleting() {
            // Arrange
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> bookService.deleteById(999L))
                .isInstanceOf(BookNotFoundException.class);

            verify(bookRepository, never()).deleteById(anyLong());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // COUNTER METRIC
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Micrometer Counter - library.books.created")
    class CounterMetricTests {

        @Test
        @DisplayName("Deve incrementar contador a cada criação")
        void shouldIncrementCounterOnCreate() {
            // Arrange
            when(bookRepository.existsByIsbn(anyString())).thenReturn(false);
            when(bookMapper.toEntity(any())).thenReturn(book);
            when(authorRepository.findAllById(anySet())).thenReturn(List.of(author));
            when(categoryRepository.findById(anyLong())).thenReturn(Optional.of(category));
            when(bookRepository.save(any(Book.class))).thenReturn(book);

            Counter counter = meterRegistry.find("library.books.created").counter();
            double before = counter == null ? 0 : counter.count();

            // Act
            bookService.create(validDTO);

            // Assert
            double after = meterRegistry.find("library.books.created").counter().count();
            assertThat(after).isEqualTo(before + 1);
        }

        @Test
        @DisplayName("Não deve incrementar contador quando criação falha")
        void shouldNotIncrementCounterOnFailure() {
            // Arrange
            when(bookRepository.existsByIsbn(validDTO.isbn())).thenReturn(true);

            Counter counter = meterRegistry.find("library.books.created").counter();
            double before = counter == null ? 0 : counter.count();

            // Act & Assert
            assertThatThrownBy(() -> bookService.create(validDTO))
                .isInstanceOf(BookAlreadyExistsException.class);

            Counter counterAfter = meterRegistry.find("library.books.created").counter();
            double after = counterAfter == null ? 0 : counterAfter.count();
            assertThat(after).isEqualTo(before);
        }
    }
}