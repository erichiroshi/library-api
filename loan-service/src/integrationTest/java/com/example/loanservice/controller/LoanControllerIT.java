package com.example.loanservice.controller;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.loanservice.client.BookClient;
import com.example.loanservice.client.UserClient;
import com.example.loanservice.client.dto.BookDTO;
import com.example.loanservice.client.dto.UserDTO;
import com.example.loanservice.loan.Loan;
import com.example.loanservice.loan.LoanItem;
import com.example.loanservice.loan.LoanRepository;
import com.example.loanservice.loan.LoanStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@ActiveProfiles("it")
@DisplayName("LoanController - Integration Tests")
class LoanControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoanRepository loanRepository;

    // ← Mockamos os Feign Clients — banco real, chamadas externas mockadas
    @MockitoBean
    private BookClient bookClient;

    @MockitoBean
    private UserClient userClient;

    private static final String USER_EMAIL = "john@example.com";
    private static final Long BOOK_ID = 1L;

    private UserDTO testUser;
    private UserDTO adminUser;
    private BookDTO availableBook;

    @BeforeEach
    void setUp() {
        testUser = new UserDTO(1L, "John Doe", USER_EMAIL, Set.of("ROLE_USER"));
        adminUser = new UserDTO(2L, "Admin", "admin@example.com", Set.of("ROLE_ADMIN"));
        availableBook = new BookDTO(BOOK_ID, "Clean Code", 5);

        // Defaults para a maioria dos testes
        when(userClient.findByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));
        when(userClient.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(bookClient.findById(BOOK_ID)).thenReturn(Optional.of(availableBook));
        when(bookClient.decrementCopies(BOOK_ID)).thenReturn(1);
    }

    // ═══════════════════════════════════════════════════════════════════
    // CREATE LOAN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/v1/loans - criar empréstimo")
    class CreateLoanTests {

        @Test
        @DisplayName("Deve criar empréstimo com sucesso")
        void shouldCreateLoan() throws Exception {
            mockMvc.perform(post("/api/v1/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": [%d]}
                        """.formatted(BOOK_ID))
                    .with(asUser()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("WAITING_RETURN"));

            assertThat(loanRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve decrementar cópias ao criar empréstimo")
        void shouldDecrementCopiesOnCreate() throws Exception {
            mockMvc.perform(post("/api/v1/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": [%d]}
                        """.formatted(BOOK_ID))
                    .with(asUser()))
                .andExpect(status().isCreated());

            // Verifica que o decrement foi chamado
            verify(bookClient).decrementCopies(BOOK_ID);
        }

        @Test
        @DisplayName("Deve retornar 409 quando livro sem cópias disponíveis")
        void shouldReturn409WhenNoCopies() throws Exception {
            when(bookClient.decrementCopies(BOOK_ID)).thenReturn(0);

            mockMvc.perform(post("/api/v1/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": [%d]}
                        """.formatted(BOOK_ID))
                    .with(asUser()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Book Not Available"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando livro não existe")
        void shouldReturn404WhenBookNotFound() throws Exception {
            when(bookClient.findById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": [999]}
                        """)
                    .with(asUser()))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Deve retornar 400 quando booksId está vazio")
        void shouldReturn400WhenBooksIdEmpty() throws Exception {
            mockMvc.perform(post("/api/v1/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": []}
                        """)
                    .with(asUser()))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve criar empréstimo com múltiplos livros")
        void shouldCreateLoanWithMultipleBooks() throws Exception {
            BookDTO book2 = new BookDTO(2L, "Refactoring", 3);
            when(bookClient.findById(2L)).thenReturn(Optional.of(book2));
            when(bookClient.decrementCopies(2L)).thenReturn(1);

            mockMvc.perform(post("/api/v1/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": [%d, 2]}
                        """.formatted(BOOK_ID))
                    .with(asUser()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.books").isArray());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET LOAN BY ID
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/loans/{id} - buscar por ID")
    class GetLoanByIdTests {

        @Test
        @DisplayName("Usuário vê o próprio empréstimo")
        void shouldGetOwnLoan() throws Exception {
            Loan loan = createLoan(testUser.id());

            mockMvc.perform(get("/api/v1/loans/{id}", loan.getId())
                    .with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.getId()));
        }

        @Test
        @DisplayName("Usuário NÃO vê empréstimo de outro — 404 intencional")
        void shouldReturn404WhenAccessingOtherUserLoan() throws Exception {
            Loan loan = createLoan(testUser.id());

            UserDTO otherUser = new UserDTO(99L, "Other", "other@example.com", Set.of("ROLE_USER"));
            when(userClient.findByEmail("other@example.com"))
                .thenReturn(Optional.of(otherUser));

            mockMvc.perform(get("/api/v1/loans/{id}", loan.getId())
            		.with(asOtherUser()))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN vê empréstimo de qualquer usuário")
        void adminShouldGetAnyLoan() throws Exception {
            Loan loan = createLoan(testUser.id());

            mockMvc.perform(get("/api/v1/loans/{id}", loan.getId())
            		.with(asAdmin()))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Deve retornar 404 quando empréstimo não existe")
        void shouldReturn404WhenLoanNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/loans/{id}", 999L)
            		.with(asUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Loan Not Found"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET MY LOANS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/v1/loans/me - meus empréstimos")
    class GetMyLoansTests {

        @Test
        @DisplayName("Retorna apenas empréstimos do usuário autenticado")
        void shouldReturnOnlyOwnLoans() throws Exception {
            createLoan(testUser.id());

            mockMvc.perform(get("/api/v1/loans/me")
            		.with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Retorna lista vazia quando não há empréstimos")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get("/api/v1/loans/me")
            		.with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // RETURN LOAN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/v1/loans/{id}/return - devolver empréstimo")
    class ReturnLoanTests {

        @Test
        @DisplayName("Deve devolver empréstimo com sucesso")
        void shouldReturnLoan() throws Exception {
            Loan loan = createLoan(testUser.id());

            mockMvc.perform(patch("/api/v1/loans/{id}/return", loan.getId())
            		.with(asUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").exists());

            // Verifica que restoreCopies foi chamado
            org.mockito.Mockito.verify(bookClient)
                .restoreCopies(BOOK_ID, 1);
        }

        @Test
        @DisplayName("Deve retornar 409 quando empréstimo já foi devolvido")
        void shouldReturn409WhenAlreadyReturned() throws Exception {
            Loan loan = createLoan(testUser.id());
            loan.setStatus(LoanStatus.RETURNED);
            loan.setReturnDate(LocalDate.now());
            loanRepository.save(loan);

            mockMvc.perform(patch("/api/v1/loans/{id}/return", loan.getId())
            		.with(asUser()))
                .andExpect(status().isConflict());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CANCEL LOAN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/v1/loans/{id}/cancel - cancelar empréstimo")
    class CancelLoanTests {

        @Test
        @DisplayName("Deve cancelar empréstimo com sucesso")
        void shouldCancelLoan() throws Exception {
            Loan loan = createLoan(testUser.id());

            mockMvc.perform(patch("/api/v1/loans/{id}/cancel", loan.getId())
            		.with(asUser()))
            .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

            org.mockito.Mockito.verify(bookClient)
                .restoreCopies(BOOK_ID, 1);
        }

        @Test
        @DisplayName("Deve retornar 409 quando empréstimo não está ativo")
        void shouldReturn409WhenNotActive() throws Exception {
            Loan loan = createLoan(testUser.id());
            loan.setStatus(LoanStatus.RETURNED);
            loanRepository.save(loan);

            mockMvc.perform(patch("/api/v1/loans/{id}/cancel", loan.getId())
            		.with(asUser()))
                .andExpect(status().isConflict());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Endpoints restritos a ADMIN")
    class AdminEndpointsTests {

        @Test
        @DisplayName("GET /api/v1/loans - ADMIN lista todos os empréstimos")
        void adminShouldListAll() throws Exception {
            createLoan(testUser.id());

            mockMvc.perform(get("/api/v1/loans")
            		.with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("GET /api/v1/loans - USER comum recebe 403")
        void userShouldReceive403() throws Exception {
            mockMvc.perform(get("/api/v1/loans")
            		.with(asUser()))
            .andDo(print())
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/v1/loans/overdue - ADMIN lista vencidos")
        void adminShouldListOverdue() throws Exception {
            Loan loan = createLoan(testUser.id());
            loan.setDueDate(LocalDate.now().minusDays(1));
            loan.setStatus(LoanStatus.WAITING_RETURN);
            loanRepository.save(loan);

            mockMvc.perform(get("/api/v1/loans/overdue")
            		.with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER
    // ═══════════════════════════════════════════════════════════════════

    private Loan createLoan(Long userId) {
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(7));
        loan.setStatus(LoanStatus.WAITING_RETURN);
        loan = loanRepository.save(loan);

        LoanItem item = new LoanItem();
        item.getId().setLoanId(loan.getId());
        item.getId().setBookId(BOOK_ID);
        item.setLoan(loan);
        item.setQuantity(1);
        loan.getItems().add(item);

        // Decrementa cópias manualmente — não passa pelo service
        when(bookClient.decrementCopies(BOOK_ID)).thenReturn(1);

        return loanRepository.save(loan);
    }
    
	private RequestPostProcessor asUser() {
		return request -> {
			request.addHeader("X-User-Id", "john@example.com");
			request.addHeader("X-User-Roles", "ROLE_USER");
			return request;
		};
	}
	
	private RequestPostProcessor asAdmin() {
		return request -> {
			request.addHeader("X-User-Id", "john@example.com");
			request.addHeader("X-User-Roles", "ROLE_ADMIN");
			return request;
		};
	}
	
	private RequestPostProcessor asOtherUser() {
		return request -> {
			request.addHeader("X-User-Id", "other@example.com");
			request.addHeader("X-User-Roles", "ROLE_USER");
			return request;
		};
	}
}