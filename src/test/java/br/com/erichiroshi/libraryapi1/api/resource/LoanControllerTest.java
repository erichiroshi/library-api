package br.com.erichiroshi.libraryapi1.api.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import br.com.erichiroshi.libraryapi1.api.dto.LoanDTO;
import br.com.erichiroshi.libraryapi1.api.dto.ReturnedLoanDTO;
import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.model.entity.Loan;
import br.com.erichiroshi.libraryapi1.service.BookService;
import br.com.erichiroshi.libraryapi1.service.LoanService;
import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@WebMvcTest(controllers = LoanController.class)
@AutoConfigureMockMvc
public class LoanControllerTest {

	  static final String LOAN_API = "/api/loans";

	    @Autowired
	    MockMvc mvc;

	    @MockBean
	    private BookService bookService;

	    @MockBean
	    private LoanService loanService;

	    @Test
	    @DisplayName("Deve realizar um emprestimo")
	    public void createLoanTest() throws Exception {
			LoanDTO dto = LoanDTO.builder().isbn("123").customer("Fulano").build();
			String json = new ObjectMapper().writeValueAsString(dto);

			Book book = Book.builder().id(1l).isbn("123").build();
			given(bookService.getBookByIsbn("123")).willReturn(Optional.of(book));

			Loan loan = Loan.builder().id(1l).customer("Fulano").book(book).loanDate(LocalDate.now()).build();
			given(loanService.save(any(Loan.class))).willReturn(loan);

			MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(LOAN_API)
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.content(json);

			mvc.perform(request)
					.andExpect(status().isCreated())
					.andExpect(content().string("1"));
		}

	    @Test
	    @DisplayName("Deve retornar erro ao tentar fazer emprestimo de um livro inexistente.")
	    public void invalidIsbnCreateLoanTest() throws  Exception{
			LoanDTO dto = LoanDTO.builder().isbn("123").customer("Fulano").build();
			String json = new ObjectMapper().writeValueAsString(dto);

			given(bookService.getBookByIsbn("123")).willReturn(Optional.empty());

			MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(LOAN_API)
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.content(json);

			mvc.perform(request)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("errors", Matchers.hasSize(1)))
					.andExpect(jsonPath("errors[0]").value("Book not found for passed isbn"));
	    }

	    @Test
	    @DisplayName("Deve retornar erro ao tentar fazer emprestimo de um livro emprestado.")
	    public void loanedBookErrorOnCreateLoanTest() throws  Exception{
			LoanDTO dto = LoanDTO.builder().isbn("123").customer("Fulano").build();
			String json = new ObjectMapper().writeValueAsString(dto);

			Book book = Book.builder().id(1l).isbn("123").build();
			given(bookService.getBookByIsbn("123")).willReturn(Optional.of(book));

			given(loanService.save(any(Loan.class))).willThrow(new BusinessException("Book already loaned"));

			MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(LOAN_API)
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.content(json);

			mvc.perform(request)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("errors", Matchers.hasSize(1)))
					.andExpect(jsonPath("errors[0]").value("Book already loaned"));
	    }
	    
	    @Test
	    @DisplayName("Deve retornar um livro")
	    public void returnBookTest() throws Exception{
	        //cenário { returned: true }
	        ReturnedLoanDTO dto = ReturnedLoanDTO.builder().returned(true).build();
	        
	        Loan loan = Loan.builder().id(1l).build();
	        given(loanService.getById(anyLong()))
	                .willReturn(Optional.of(loan));

	        String json = new ObjectMapper().writeValueAsString(dto);

	        mvc.perform(
	            patch(LOAN_API.concat("/1"))
	            .accept(MediaType.APPLICATION_JSON)
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(json)
	        ).andExpect( status().isOk() );

	        verify(loanService, times(1)).update(loan);
	    }
	    
	    @Test
	    @DisplayName("Deve retornar 404 quando tentar devolver um livro inexistente.")
	    public void returnInexistentBookTest() throws Exception{
	        //cenário
	        ReturnedLoanDTO dto = ReturnedLoanDTO.builder().returned(true).build();
	        String json = new ObjectMapper().writeValueAsString(dto);

	        given(loanService.getById(anyLong())).willReturn(Optional.empty());

	        mvc.perform(
	                patch(LOAN_API.concat("/1"))
	                        .accept(MediaType.APPLICATION_JSON)
	                        .contentType(MediaType.APPLICATION_JSON)
	                        .content(json)
	        ).andExpect( status().isNotFound() );
	    }
	    
	    

	}