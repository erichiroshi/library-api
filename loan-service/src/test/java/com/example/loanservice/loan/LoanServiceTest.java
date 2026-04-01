package com.example.loanservice.loan;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.loanservice.client.BookClient;
import com.example.loanservice.client.UserClient;
import com.example.loanservice.client.dto.BookDTO;
import com.example.loanservice.client.dto.UserDTO;
import com.example.loanservice.loan.dto.LoanCreateDTO;
import com.example.loanservice.loan.dto.LoanResponseDTO;
import com.example.loanservice.loan.exception.BookNotAvailableException;
import com.example.loanservice.loan.exception.LoanAlreadyReturnedException;
import com.example.loanservice.loan.exception.LoanNotFoundException;
import com.example.loanservice.loan.exception.LoanUnauthorizedException;
import com.example.loanservice.loan.mapper.LoanMapper;
import com.example.loanservice.messaging.LoanEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService - Unit Tests (Microservice)")
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock BookClient bookClient;
    @Mock UserClient userClient;
    @Mock LoanMapper mapper;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock LoanEventPublisher loanEventPublisher; 

    @InjectMocks
    LoanService loanService;

    private UserDTO authenticatedUser;
    private UserDTO adminUser;
    private BookDTO availableBook;
    private Loan activeLoan;
    UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        authenticatedUser = new UserDTO(1L, "John Doe", "john@example.com", Set.of("ROLE_USER"));
        adminUser = new UserDTO(2L, "Admin", "admin@example.com", Set.of("ROLE_ADMIN"));

        availableBook = new BookDTO(1L, "Clean Code", 5);

        activeLoan = new Loan();
        activeLoan.setId(1L);
        activeLoan.setUserId(1L);
        activeLoan.setLoanDate(LocalDate.now());
        activeLoan.setDueDate(LocalDate.now().plusDays(7));
        activeLoan.setStatus(LoanStatus.WAITING_RETURN);
    }
    
    @Nested
    @DisplayName("create() - criar empréstimo")
    class CreateLoanTests {

        @Test
        @DisplayName("Deve criar empréstimo com sucesso")
        void shouldCreateLoanSuccessfully() {
            LoanCreateDTO dto = new LoanCreateDTO(Set.of(1L));
            LoanResponseDTO expectedResponse = new LoanResponseDTO(
                1L, LocalDate.now(), LocalDate.now().plusDays(7),
                null, LoanStatus.WAITING_RETURN, "John Doe", Set.of()
            );

            when(userClient.findByEmail("john@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
            when(bookClient.findInternalBooksById(1L)).thenReturn(Optional.of(availableBook));
            when(bookClient.decrementInternalCopies(1L)).thenReturn(1);
            when(loanRepository.save(any(Loan.class))).thenAnswer(i -> {
                Loan loan = i.getArgument(0);
                loan.setId(1L);
                return loan;
            });
            when(loanRepository.findByIdWithItems(1L)).thenReturn(Optional.of(activeLoan));
            when(mapper.toDTO(any(Loan.class))).thenReturn(expectedResponse);
            
            setAuthentication(authenticatedUser);

            LoanResponseDTO result = loanService.create(dto);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(LoanStatus.WAITING_RETURN);
            verify(bookClient).decrementInternalCopies(1L);
            verify(loanRepository).save(any(Loan.class));
        }

        @Test
        @DisplayName("Deve lançar BookNotAvailableException quando não há cópias")
        void shouldThrowBookNotAvailableException() {
            LoanCreateDTO dto = new LoanCreateDTO(Set.of(1L));

            when(userClient.findByEmail("john@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
            when(bookClient.findInternalBooksById(1L)).thenReturn(Optional.of(availableBook));
            when(bookClient.decrementInternalCopies(1L)).thenReturn(0);
            
            setAuthentication(authenticatedUser);

            assertThatThrownBy(() -> loanService.create(dto))
                .isInstanceOf(BookNotAvailableException.class);

            verify(loanRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("returnLoan() - devolver empréstimo")
    class ReturnLoanTests {

        @Test
        @DisplayName("Deve devolver empréstimo com sucesso")
        void shouldReturnLoanSuccessfully() {
            when(userClient.findByEmail("john@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
            when(loanRepository.findByIdWithItems(1L))
                .thenReturn(Optional.of(activeLoan));

            setAuthentication(authenticatedUser);
            
            loanService.returnLoan(1L);

            assertThat(activeLoan.getStatus()).isEqualTo(LoanStatus.RETURNED);
            assertThat(activeLoan.getReturnDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Deve lançar LoanNotFoundException quando não existe")
        void shouldThrowLoanNotFoundException() {
            when(userClient.findByEmail("john@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
            when(loanRepository.findByIdWithItems(999L))
                .thenReturn(Optional.empty());
            
            setAuthentication(authenticatedUser);

            assertThatThrownBy(() -> loanService.returnLoan(999L))
                .isInstanceOf(LoanNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lançar LoanUnauthorizedException quando usuário não é dono")
        void shouldThrowLoanUnauthorizedException() {
            UserDTO otherUser = new UserDTO(3L, "Other", "other@example.com", Set.of("ROLE_USER"));

            when(userClient.findByEmail("other@example.com"))
                .thenReturn(Optional.of(otherUser));
            when(loanRepository.findByIdWithItems(1L))
                .thenReturn(Optional.of(activeLoan));
            
            setAuthentication(otherUser);

            assertThatThrownBy(() -> loanService.returnLoan(1L))
                .isInstanceOf(LoanUnauthorizedException.class);
        }

        @Test
        @DisplayName("ADMIN deve devolver empréstimo de qualquer usuário")
        void adminShouldReturnAnyLoan() {
            when(userClient.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(adminUser));
            when(loanRepository.findByIdWithItems(1L))
                .thenReturn(Optional.of(activeLoan));

            setAuthentication(adminUser);

            loanService.returnLoan(1L);

            assertThat(activeLoan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        }

        @Test
        @DisplayName("Deve lançar LoanAlreadyReturnedException quando já devolvido")
        void shouldThrowWhenAlreadyReturned() {
            activeLoan.setStatus(LoanStatus.RETURNED);

            when(userClient.findByEmail("john@example.com"))
                .thenReturn(Optional.of(authenticatedUser));
            when(loanRepository.findByIdWithItems(1L))
                .thenReturn(Optional.of(activeLoan));
            activeLoan.setStatus(LoanStatus.RETURNED);
            
            setAuthentication(authenticatedUser);

            assertThatThrownBy(() -> loanService.returnLoan(1L))
                .isInstanceOf(LoanAlreadyReturnedException.class);
        }
    }

    @Nested
    @DisplayName("findMyLoans() - meus empréstimos")
    class FindMyLoansTests {

        @Test
        @DisplayName("Deve retornar empréstimos do usuário autenticado")
        void shouldReturnAuthenticatedUserLoans() {
            when(userClient.findByEmail("john@example.com"))
            .thenReturn(Optional.of(authenticatedUser));
            when(loanRepository.findByUserIdWithItems(1L))
                .thenReturn(List.of(activeLoan));

            setAuthentication(authenticatedUser);
            
            loanService.findMyLoans();

            verify(loanRepository).findByUserIdWithItems(1L);
        }
    }
 
// ═══════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════
    
	private void setAuthentication(UserDTO user) {
		String userEmail = user.email();
		String roles = user.roles().toString();

		List<SimpleGrantedAuthority> authorities = Arrays.stream(roles.split(",")).map(String::trim)
				.map(SimpleGrantedAuthority::new).toList();

		authentication = new UsernamePasswordAuthenticationToken(userEmail, null, authorities);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}