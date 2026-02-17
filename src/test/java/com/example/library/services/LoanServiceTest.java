package com.example.library.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.book.exception.BookNotFoundException;
import com.example.library.category.Category;
import com.example.library.loan.Loan;
import com.example.library.loan.LoanRepository;
import com.example.library.loan.LoanService;
import com.example.library.loan.LoanStatus;
import com.example.library.loan.dto.LoanCreateDTO;
import com.example.library.loan.dto.LoanResponseDTO;
import com.example.library.loan.execption.BookNotAvailableException;
import com.example.library.loan.execption.LoanAlreadyReturnedException;
import com.example.library.loan.execption.LoanNotFoundException;
import com.example.library.loan.execption.LoanUnauthorizedException;
import com.example.library.loan.mapper.LoanMapper;
import com.example.library.user.User;
import com.example.library.user.UserRepository;
import com.example.library.user.exception.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService - Unit Tests")
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoanMapper mapper;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private LoanService loanService;

    private User authenticatedUser;
    private User adminUser;
    private Book availableBook;
    private Category category;
    private Loan activeLoan;

    @BeforeEach
    void setUp() {
        // Usuário comum autenticado
        authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setName("John Doe");
        authenticatedUser.setEmail("john@example.com");
        authenticatedUser.setRoles(Set.of("ROLE_USER"));

        // Admin
        adminUser = new User();
        adminUser.setId(2L);
        adminUser.setName("Admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setRoles(Set.of("ROLE_ADMIN"));

        // Categoria
        category = new Category();
        category.setId(1L);
        category.setName("Fiction");

        // Livro com cópias disponíveis
        availableBook = new Book();
        availableBook.setId(1L);
        availableBook.setTitle("Clean Code");
        availableBook.setIsbn("978-0132350884");
        availableBook.setAvailableCopies(5);
        availableBook.setCategory(category);

        // Empréstimo ativo
        activeLoan = new Loan();
        activeLoan.setId(1L);
        activeLoan.setUser(authenticatedUser);
        activeLoan.setLoanDate(LocalDate.now());
        activeLoan.setDueDate(LocalDate.now().plusDays(7));
        activeLoan.setStatus(LoanStatus.WAITING_RETURN);

        // Mock SecurityContext
        SecurityContextHolder.setContext(securityContext);
    }

    // ═══════════════════════════════════════════════════════════════════
    // CREATE LOAN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create() - criar empréstimo")
    class CreateLoanTests {

        @Test
        @DisplayName("Deve criar empréstimo com sucesso")
        void shouldCreateLoanSuccessfully() {
            // Arrange
            LoanCreateDTO dto = new LoanCreateDTO(Set.of(1L));
            LoanResponseDTO expectedResponse = new LoanResponseDTO(
                1L, LocalDate.now(), LocalDate.now().plusDays(7),
                null, LoanStatus.WAITING_RETURN, "John Doe", Set.of()
            );

            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);

            when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
            when(bookRepository.decrementCopies(1L)).thenReturn(1);
            when(loanRepository.save(any(Loan.class))).thenAnswer(i -> {
                Loan loan = i.getArgument(0);
                loan.setId(1L);
                return loan;
            });
            when(mapper.toDTO(any(Loan.class))).thenReturn(expectedResponse);

            // Act
            LoanResponseDTO result = loanService.create(dto);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.status()).isEqualTo(LoanStatus.WAITING_RETURN);

            verify(bookRepository).decrementCopies(1L);
            verify(loanRepository).save(any(Loan.class));

            // Verifica que o loan foi criado com os campos corretos
            ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
            verify(loanRepository).save(loanCaptor.capture());
            Loan savedLoan = loanCaptor.getValue();

            assertThat(savedLoan.getUser()).isEqualTo(authenticatedUser);
            assertThat(savedLoan.getStatus()).isEqualTo(LoanStatus.WAITING_RETURN);
            assertThat(savedLoan.getDueDate()).isEqualTo(LocalDate.now().plusDays(7));
            assertThat(savedLoan.getItems()).hasSize(1);
        }

        @Test
        @DisplayName("Deve lançar BookNotFoundException quando livro não existe")
        void shouldThrowBookNotFoundException() {
            // Arrange
            LoanCreateDTO dto = new LoanCreateDTO(Set.of(999L));

            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> loanService.create(dto))
                .isInstanceOf(BookNotFoundException.class);

            verify(bookRepository, never()).decrementCopies(anyLong());
            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar BookNotAvailableException quando não há cópias disponíveis")
        void shouldThrowBookNotAvailableException() {
            // Arrange
            LoanCreateDTO dto = new LoanCreateDTO(Set.of(1L));

            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
            when(bookRepository.decrementCopies(1L)).thenReturn(0); // Sem cópias disponíveis

            // Act & Assert
            assertThatThrownBy(() -> loanService.create(dto))
                .isInstanceOf(BookNotAvailableException.class);

            verify(loanRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar empréstimo com múltiplos livros")
        void shouldCreateLoanWithMultipleBooks() {
            // Arrange
            Book book2 = new Book();
            book2.setId(2L);
            book2.setTitle("Refactoring");
            book2.setIsbn("978-0134757599");
            book2.setAvailableCopies(3);
            book2.setCategory(category);

            LoanCreateDTO dto = new LoanCreateDTO(Set.of(1L, 2L));

            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);

            when(bookRepository.findById(1L)).thenReturn(Optional.of(availableBook));
            when(bookRepository.findById(2L)).thenReturn(Optional.of(book2));
            when(bookRepository.decrementCopies(1L)).thenReturn(1);
            when(bookRepository.decrementCopies(2L)).thenReturn(1);
            when(loanRepository.save(any(Loan.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            loanService.create(dto);

            // Assert
            verify(bookRepository).decrementCopies(1L);
            verify(bookRepository).decrementCopies(2L);

            ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
            verify(loanRepository).save(loanCaptor.capture());
            assertThat(loanCaptor.getValue().getItems()).hasSize(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // RETURN LOAN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("returnLoan() - devolver empréstimo")
    class ReturnLoanTests {

        @Test
        @DisplayName("Deve devolver empréstimo com sucesso")
        void shouldReturnLoanSuccessfully() {
            // Arrange
            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

            // Act
            loanService.returnLoan(1L);

            // Assert
            assertThat(activeLoan.getStatus()).isEqualTo(LoanStatus.RETURNED);
            assertThat(activeLoan.getReturnDate()).isEqualTo(LocalDate.now());
            verify(mapper).toDTO(activeLoan);
        }

        @Test
        @DisplayName("Deve lançar LoanNotFoundException quando empréstimo não existe")
        void shouldThrowLoanNotFoundException() {
            // Arrange
            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> loanService.returnLoan(999L))
                .isInstanceOf(LoanNotFoundException.class);
        }

        @Test
        @DisplayName("Deve lançar LoanAlreadyReturnedException quando já foi devolvido")
        void shouldThrowLoanAlreadyReturnedException() {
            // Arrange
            activeLoan.setStatus(LoanStatus.RETURNED);
            activeLoan.setReturnDate(LocalDate.now().minusDays(1));

            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

            // Act & Assert
            assertThatThrownBy(() -> loanService.returnLoan(1L))
                .isInstanceOf(LoanAlreadyReturnedException.class);
        }

        @Test
        @DisplayName("Deve lançar LoanUnauthorizedException quando usuário tenta devolver empréstimo de outro")
        void shouldThrowLoanUnauthorizedException() {
            // Arrange
            User otherUser = new User();
            otherUser.setId(3L);
            otherUser.setName("Other User");
            otherUser.setRoles(Set.of("ROLE_USER"));

            Authentication auth = new UsernamePasswordAuthenticationToken(otherUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

            // Act & Assert
            assertThatThrownBy(() -> loanService.returnLoan(1L))
                .isInstanceOf(LoanUnauthorizedException.class);
        }

        @Test
        @DisplayName("ADMIN deve conseguir devolver empréstimo de qualquer usuário")
        void adminShouldReturnAnyLoan() {
            // Arrange
            Authentication auth = new UsernamePasswordAuthenticationToken(adminUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

            // Act & Assert
            assertThatCode(() -> loanService.returnLoan(1L))
                .doesNotThrowAnyException();

            assertThat(activeLoan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CANCEL LOAN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("cancelLoan() - cancelar empréstimo")
    class CancelLoanTests {

        @Test
        @DisplayName("Deve cancelar empréstimo com sucesso")
        void shouldCancelLoanSuccessfully() {
            // Arrange
            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

            // Act
            loanService.cancelLoan(1L);

            // Assert
            assertThat(activeLoan.getStatus()).isEqualTo(LoanStatus.CANCELED);
        }

        @Test
        @DisplayName("Deve lançar exceção ao tentar cancelar empréstimo já devolvido")
        void shouldThrowExceptionWhenCancelingReturnedLoan() {
            // Arrange
            activeLoan.setStatus(LoanStatus.RETURNED);

            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

            // Act & Assert
            assertThatThrownBy(() -> loanService.cancelLoan(1L))
                .isInstanceOf(LoanAlreadyReturnedException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FIND METHODS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findById() - buscar por ID")
    class FindByIdTests {

        @Test
        @DisplayName("Deve buscar empréstimo do próprio usuário")
        void shouldFindOwnLoan() {
            // Arrange
            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

            // Act & Assert
            assertThatCode(() -> loanService.findById(1L))
                .doesNotThrowAnyException();

            verify(mapper).toDTO(activeLoan);
        }

        @Test
        @DisplayName("ADMIN deve buscar empréstimo de qualquer usuário")
        void adminShouldFindAnyLoan() {
            // Arrange
            Authentication auth = new UsernamePasswordAuthenticationToken(adminUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findById(1L)).thenReturn(Optional.of(activeLoan));

            // Act & Assert
            assertThatCode(() -> loanService.findById(1L))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("findMyLoans() - buscar meus empréstimos")
    class FindMyLoansTests {

        @Test
        @DisplayName("Deve retornar lista de empréstimos do usuário autenticado")
        void shouldReturnAuthenticatedUserLoans() {
            // Arrange
            Authentication auth = new UsernamePasswordAuthenticationToken(authenticatedUser, null);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loanRepository.findByUserId(1L)).thenReturn(List.of(activeLoan));

            // Act
            loanService.findMyLoans();

            // Assert
            verify(loanRepository).findByUserId(1L);
            verify(mapper).toDTO(activeLoan);
        }
    }

    @Nested
    @DisplayName("findByUser() - buscar por usuário")
    class FindByUserTests {

        @Test
        @DisplayName("Deve retornar empréstimos de usuário específico")
        void shouldReturnUserLoans() {
            // Arrange
            when(userRepository.findById(1L)).thenReturn(Optional.of(authenticatedUser));
            when(loanRepository.findByUserId(1L)).thenReturn(List.of(activeLoan));

            // Act
            loanService.findByUser(1L);

            // Assert
            verify(loanRepository).findByUserId(1L);
        }

        @Test
        @DisplayName("Deve lançar UserNotFoundException quando usuário não existe")
        void shouldThrowUserNotFoundException() {
            // Arrange
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> loanService.findByUser(999L))
                .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findOverdue() - buscar vencidos")
    class FindOverdueTests {

        @Test
        @DisplayName("Deve retornar empréstimos vencidos")
        void shouldReturnOverdueLoans() {
            // Arrange
            Loan overdueLoan = new Loan();
            overdueLoan.setId(2L);
            overdueLoan.setDueDate(LocalDate.now().minusDays(1));
            overdueLoan.setStatus(LoanStatus.WAITING_RETURN);

            when(loanRepository.findOverdueLoans(any(LocalDate.class)))
                .thenReturn(List.of(overdueLoan));

            // Act
            loanService.findOverdue();

            // Assert
            verify(loanRepository).findOverdueLoans(any(LocalDate.class));
        }
    }

    @Nested
    @DisplayName("markOverdue() - marcar como vencido")
    class MarkOverdueTests {

        @Test
        @DisplayName("Deve marcar empréstimos vencidos como OVERDUE")
        void shouldMarkLoansAsOverdue() {
            // Arrange
            Loan overdueLoan = new Loan();
            overdueLoan.setId(2L);
            overdueLoan.setDueDate(LocalDate.now().minusDays(1));
            overdueLoan.setStatus(LoanStatus.WAITING_RETURN);

            when(loanRepository.findOverdueLoans(any(LocalDate.class)))
                .thenReturn(List.of(overdueLoan));

            // Act
            loanService.markOverdue();

            // Assert
            assertThat(overdueLoan.getStatus()).isEqualTo(LoanStatus.OVERDUE);
        }
    }
}