package com.example.library.book;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
 
import java.util.Optional;
 
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import com.example.library.book.exception.BookNotFoundException;
 
@ExtendWith(MockitoExtension.class)
@DisplayName("BookService - updateCoverImageUrl()")
class BookServiceUpdateCoverImageUrlTest {
 
    @Mock
    private BookRepository repository;
 
    @InjectMocks
    private BookService bookService;
 
    private Book book;
 
    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(1L);
        book.setTitle("Clean Code");
        book.setIsbn("978-0132350884");
        book.setPublicationYear(2008);
        book.setAvailableCopies(3);
    }
 
    @Nested
    @DisplayName("Casos de Sucesso")
    class SuccessCases {
 
        @Test
        @DisplayName("Deve atualizar coverImageUrl e salvar o livro")
        void shouldUpdateCoverImageUrlAndSave() {
            when(repository.findById(1L)).thenReturn(Optional.of(book));
 
            bookService.updateCoverImageUrl(1L, "book-1");
 
            ArgumentCaptor<Book> captor = forClass(Book.class);
            verify(repository).save(captor.capture());
 
            assertThat(captor.getValue().getCoverImageUrl()).isEqualTo("book-1");
        }
 
        @Test
        @DisplayName("Deve sobrescrever URL existente com a nova")
        void shouldOverwriteExistingCoverImageUrl() {
            book.setCoverImageUrl("old-book-1");
            when(repository.findById(1L)).thenReturn(Optional.of(book));
 
            bookService.updateCoverImageUrl(1L, "new-book-1");
 
            ArgumentCaptor<Book> captor = forClass(Book.class);
            verify(repository).save(captor.capture());
 
            assertThat(captor.getValue().getCoverImageUrl())
                    .isEqualTo("new-book-1")
                    .isNotEqualTo("old-book-1");
        }
 
        @Test
        @DisplayName("Deve manter os demais campos do livro inalterados")
        void shouldKeepOtherFieldsUnchanged() {
            when(repository.findById(1L)).thenReturn(Optional.of(book));
 
            bookService.updateCoverImageUrl(1L, "book-1");
 
            ArgumentCaptor<Book> captor = forClass(Book.class);
            verify(repository).save(captor.capture());
 
            Book saved = captor.getValue();
            assertThat(saved.getTitle()).isEqualTo("Clean Code");
            assertThat(saved.getIsbn()).isEqualTo("978-0132350884");
            assertThat(saved.getPublicationYear()).isEqualTo(2008);
            assertThat(saved.getAvailableCopies()).isEqualTo(3);
        }
 
        @Test
        @DisplayName("Deve aceitar fileName com ID grande")
        void shouldAcceptFileNameWithLargeId() {
            Book bookLarge = new Book();
            bookLarge.setId(99999L);
            when(repository.findById(99999L)).thenReturn(Optional.of(bookLarge));
 
            bookService.updateCoverImageUrl(99999L, "book-99999");
 
            ArgumentCaptor<Book> captor = forClass(Book.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getCoverImageUrl()).isEqualTo("book-99999");
        }
    }
 
    @Nested
    @DisplayName("Casos de Erro")
    class ErrorCases {
 
        @Test
        @DisplayName("Deve lançar BookNotFoundException quando livro não existe")
        void shouldThrowBookNotFoundExceptionWhenBookDoesNotExist() {
            when(repository.findById(999L)).thenReturn(Optional.empty());
 
            assertThatThrownBy(() -> bookService.updateCoverImageUrl(999L, "book-999"))
                    .isInstanceOf(BookNotFoundException.class);
 
            verify(repository, never()).save(any());
        }
    }
}