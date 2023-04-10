package br.com.erichiroshi.libraryapi1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.model.entity.Loan;
import br.com.erichiroshi.libraryapi1.model.repository.LoanRepository;
import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;
import br.com.erichiroshi.libraryapi1.service.impl.LoanServiceImpl;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class LoanServiceTest {

    LoanService service;

    @MockBean
    LoanRepository repository;

    @BeforeEach
    public void setUp(){
        this.service = new LoanServiceImpl(repository);
    }

    @Test
    @DisplayName("Deve salvar um empréstimo")
    public void saveLoanTest(){
        Book book = Book.builder().id(1l).build();
        String customer = "Fulano";

        Loan savingLoan =
                Loan.builder()
                .book(book)
                .customer(customer)
                .loanDate(LocalDate.now())
                .build();

        Loan savedLoan = Loan.builder()
                    .id(1l)
                    .loanDate(LocalDate.now())
                    .customer(customer)
                    .book(book).build();


        when( repository.existsByBookAndNotReturned(book) ).thenReturn(false);
        when( repository.save(savingLoan) ).thenReturn( savedLoan );

        Loan loan = service.save(savingLoan);

        assertThat(loan.getId()).isEqualTo(savedLoan.getId());
        assertThat(loan.getBook().getId()).isEqualTo(savedLoan.getBook().getId());
        assertThat(loan.getCustomer()).isEqualTo(savedLoan.getCustomer());
        assertThat(loan.getLoanDate()).isEqualTo(savedLoan.getLoanDate());
    }

    @Test
    @DisplayName("Deve lançar erro de negócio ao salvar um empréstimo com livro já emprestado")
    public void loanedBookSaveTest(){
        Book book = Book.builder().id(1l).build();
        String customer = "Fulano";

        Loan savingLoan =
                Loan.builder()
                        .book(book)
                        .customer(customer)
                        .loanDate(LocalDate.now())
                        .build();

        when(repository.existsByBookAndNotReturned(book)).thenReturn(true);

        Throwable exception = catchThrowable(() -> service.save(savingLoan));

        assertThat(exception)
                .isInstanceOf(BusinessException.class)
                .hasMessage("Book already loaned");

        verify(repository, never()).save(savingLoan);
    }
    
    @Test
    @DisplayName(" Deve obter as informações de um empréstimo pelo ID")
	public void getLoanDetaisTest() {
		// cenário
		Long id = 1l;

		Loan loan = createLoan();
		loan.setId(id);

		when(repository.findById(id)).thenReturn(Optional.of(loan));

		// execucao
		Optional<Loan> result = service.getById(id);

		// verificacao
		assertThat(result.isPresent()).isTrue();
		assertThat(result.get().getId()).isEqualTo(id);
		assertThat(result.get().getCustomer()).isEqualTo(loan.getCustomer());
		assertThat(result.get().getBook()).isEqualTo(loan.getBook());
		assertThat(result.get().getLoanDate()).isEqualTo(loan.getLoanDate());

		verify(repository).findById(id);
	}

	public static Loan createLoan() {
		Book book = Book.builder().id(1l).build();
		String customer = "Fulano";

	      return Loan.builder()
                        .book(book)
                        .customer(customer)
                        .loanDate(LocalDate.now())
                        .build();
    }
    
}