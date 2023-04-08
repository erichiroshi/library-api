package br.com.erichiroshi.libraryapi1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.model.repository.BookRepository;
import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;
import br.com.erichiroshi.libraryapi1.service.impl.BookServiceImpl;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class BookServiceTest {

	BookService service;

	@MockBean
	BookRepository repository;

	@BeforeEach
	public void setup() {
		this.service = new BookServiceImpl(repository);
	}

	@Test
	@DisplayName("Deve salvar um livro.")
	public void saveBookTest() {
		Book book = createValidBook();
		when(repository.existsByIsbn(anyString())).thenReturn(false);
		BDDMockito.when(repository.save(book))
				.thenReturn(Book.builder().id(1L).author("Fulano").title("As aventuras").isbn("123").build());

		Book savedBook = service.save(book);

		assertThat(savedBook.getId()).isNotNull();
		assertThat(savedBook.getAuthor()).isEqualTo("Fulano");
		assertThat(savedBook.getTitle()).isEqualTo("As aventuras");
		assertThat(savedBook.getIsbn()).isEqualTo("123");
	}

	@Test
	@DisplayName("Deve lançar erro de negocio ao tentar salvar um livro com isbn duplicado")
	public void shouldNotSaveABookWithDuplicatedISBN() {
		Book book = createValidBook();
		when(repository.existsByIsbn(anyString())).thenReturn(true);
		
		Throwable exception = Assertions.catchThrowable(() -> service.save(book));
		
		assertThat(exception)
				.isInstanceOf(BusinessException.class)
				.hasMessage("Isbn já cadastrado.");

		verify(repository, never()).save(book);
	}

	@Test
	@DisplayName("Deve obter um livro por Id")
	public void getByIdTest() {
		Long id = 1l;
		Book book = createValidBook();
		book.setId(id);

		when(repository.findById(id)).thenReturn(Optional.of(book));

		Optional<Book> foundBook = service.getById(id);

		assertThat(foundBook.isPresent()).isTrue();
		assertThat(foundBook.get().getId()).isEqualTo(id);
		assertThat(foundBook.get().getAuthor()).isEqualTo(book.getAuthor());
		assertThat(foundBook.get().getIsbn()).isEqualTo(book.getIsbn());
		assertThat(foundBook.get().getTitle()).isEqualTo(book.getTitle());
	}

	@Test
	@DisplayName("Deve retornar vazio ao obter um livro por Id quando ele não existe na base.")
	public void bookNotFoundByIdTest() {
		Long id = 1l;
		when(repository.findById(id)).thenReturn(Optional.empty());

		Optional<Book> book = service.getById(id);

		assertThat(book.isPresent()).isFalse();

	}

	@Test
	@DisplayName("Deve deletar um livro.")
	public void deleteBookTest() {
		Book book = Book.builder().id(1l).build();

		assertDoesNotThrow(() -> service.delete(book));

		verify(repository, times(1)).delete(book);
	}

	@Test
	@DisplayName("Deve ocorrer erro ao tentar deletar um livro inexistente.")
	public void deleteInvalidBookTest() {
		Book book = new Book();

		assertThrows(IllegalArgumentException.class, () -> service.delete(book));

		verify(repository, never()).delete(book);
	}

	private Book createValidBook() {
		return Book.builder().author("Fulano").title("As aventuras").isbn("123").build();
	}

	@Test
	@DisplayName("Deve ocorrer erro ao tentar atualizar um livro inexistente.")
	public void updateInvalidBookTest() {
		Book book = new Book();

		assertThrows(IllegalArgumentException.class, () -> service.update(book));

		verify(repository, Mockito.never()).save(book);
	}

	@Test
	@DisplayName("Deve atualizar um livro.")
	public void updateBookTest() {
		// cenário
		long id = 1l;

		// livro a atualizar
		Book updatingBook = Book.builder().id(id).build();

		// simulacao
		Book updatedBook = createValidBook();
		updatedBook.setId(id);
		when(repository.save(updatingBook)).thenReturn(updatedBook);

		// exeucao
		Book book = service.update(updatingBook);

		// verificacoes
		assertThat(book.getId()).isEqualTo(updatedBook.getId());
		assertThat(book.getTitle()).isEqualTo(updatedBook.getTitle());
		assertThat(book.getIsbn()).isEqualTo(updatedBook.getIsbn());
		assertThat(book.getAuthor()).isEqualTo(updatedBook.getAuthor());

	}

}
