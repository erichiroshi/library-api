package br.com.erichiroshi.libraryapi1.api.resource;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.erichiroshi.libraryapi1.api.dto.BookDTO;
import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.service.BookService;
import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@WebMvcTest
@AutoConfigureMockMvc
public class BookControllerTest {

	static String BOOK_API = "/api/books";

	@Autowired
	MockMvc mvc;
	
	@MockBean
	BookService service;

	@Test
	@DisplayName("Deve criar um livro com sucesso.")
	public void createBookTest() throws Exception {
		BookDTO bookDTO = createNewBookDTO();
		Book savedBook = Book.builder().id(10L).author("Autor").title("As aventuras").isbn("001").build();

		BDDMockito.given(service.save(any(Book.class))).willReturn(savedBook);
		String json = new ObjectMapper().writeValueAsString(bookDTO);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.post(BOOK_API)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(json);

		mvc
			.perform(request)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("id").value(10L))
			.andExpect(jsonPath("title").value(bookDTO.getTitle()))
			.andExpect(jsonPath("author").value(bookDTO.getAuthor()))
			.andExpect(jsonPath("isbn").value(bookDTO.getIsbn()));
	}

	@Test
	@DisplayName("Deve lançar erro de validação quando não houver dados suficiente para criação do livro.")
	public void createInvalidBookTest() throws Exception {

		String json = new ObjectMapper().writeValueAsString(new BookDTO());
		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.post(BOOK_API)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(json);
		
		mvc
		.perform(request)
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("errors", hasSize(3)));
	}
	
	@Test
	@DisplayName("Deve lançar erro ao tentar cadastrar um livro com isbn já utilizado por outro")
	public void createBookWithDuplicateIsbn() throws Exception {
		BookDTO bookDTO = createNewBookDTO();
		String json = new ObjectMapper().writeValueAsString(bookDTO);
		String mensagemErro = "Isbn já cadastrado";
		when(service.save(any(Book.class))).thenThrow(new BusinessException(mensagemErro));
		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.post(BOOK_API)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.content(json);
		
		mvc
		.perform(request)
		.andExpect(status().isBadRequest())
		.andExpect(jsonPath("errors", hasSize(1)))
		.andExpect(jsonPath("errors[0]").value(mensagemErro));
	}
	
	@Test
	@DisplayName("Deve obter informacoes de um livro")
	public void getBookDetailsTest() throws Exception {
		Long id = 1L;
		Book book = Book.builder()
				.id(id)
				.title(createNewBookDTO().getTitle())
				.author(createNewBookDTO().getAuthor())
				.isbn(createNewBookDTO().getIsbn()).build();
		
		BDDMockito.given(service.getById(id)).willReturn(Optional.of(book));
		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.get(BOOK_API.concat("/" + id))
				.accept(MediaType.APPLICATION_JSON);
		
		mvc
		.perform(request)
		.andExpect(status().isOk())
		.andExpect(jsonPath("id").value(id))
		.andExpect(jsonPath("title").value(createNewBookDTO().getTitle()))
		.andExpect(jsonPath("author").value(createNewBookDTO().getAuthor()))
		.andExpect(jsonPath("isbn").value(createNewBookDTO().getIsbn()));
	}
	
	@Test
	@DisplayName("Deve retonar resource not found quando o livro procurado não existir")
	public void getNotFoundTest() throws Exception {
		Long id = 1L;
		
		BDDMockito.given(service.getById(Mockito.anyLong())).willReturn(Optional.empty());
		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.get(BOOK_API.concat("/" + id))
				.accept(MediaType.APPLICATION_JSON);
		
		mvc
		.perform(request)
		.andExpect(status().isNotFound());
	}
	
	@Test
	@DisplayName("Deve deletar um livro")
	public void deleteBookTest() throws Exception {

		BDDMockito.given(service.getById(anyLong())).willReturn(Optional.of(Book.builder().id(1L).build()));

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
						.delete(BOOK_API.concat("/" + 1));

		mvc.perform(request)
				.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("Deve retonar resource not found quando não encontrar o livro para deletar")
	public void deleteInexistentBookTest() throws Exception {
		
		BDDMockito.given(service.getById(anyLong())).willReturn(Optional.empty());
		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders
				.delete(BOOK_API.concat("/" + 1));
		
		mvc.perform(request)
		.andExpect(status().isNotFound());
	}
	
	private BookDTO createNewBookDTO() {
		return BookDTO.builder().author("Autor").title("As aventuras").isbn("001").build();
	}
}
