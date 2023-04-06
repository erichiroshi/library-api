package br.com.erichiroshi.libraryapi1.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import br.com.erichiroshi.libraryapi1.api.service.impl.BookServiceImpl;
import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.model.repository.BookRepository;
import br.com.erichiroshi.libraryapi1.service.BookService;

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
		Book book = Book.builder().author("Fulano").title("As aventuras").isbn("123").build();
		BDDMockito.when(repository.save(book)).thenReturn(
				Book.builder().id(1L)
						.author("Fulano")
						.title("As aventuras")
						.isbn("123").build());

		Book savedBook = service.save(book);

		assertThat(savedBook.getId()).isNotNull();
		assertThat(savedBook.getAuthor()).isEqualTo("Fulano");
		assertThat(savedBook.getTitle()).isEqualTo("As aventuras");
		assertThat(savedBook.getIsbn()).isEqualTo("123");
	}
}
