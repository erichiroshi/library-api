package com.example.library.controller;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.library.author.Author;
import com.example.library.author.AuthorRepository;
import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.category.Category;
import com.example.library.category.CategoryRepository;
import com.example.library.config.BaseControllerIT;
import com.example.library.loan.Loan;
import com.example.library.loan.LoanItem;
import com.example.library.loan.LoanItemId;
import com.example.library.loan.LoanRepository;
import com.example.library.loan.LoanStatus;
import com.example.library.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@ActiveProfiles("it")
@DisplayName("LoanController - Integration Tests")
class LoanControllerIT extends BaseControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LoanRepository loanRepository;

	@Autowired
	private BookRepository bookRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private AuthorRepository authorRepository;
	
    private Category category;
    private Author author;
    private Book availableBook;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("Technology");
        category = categoryRepository.save(category);

        author = new Author();
        author.setName("Robert C. Martin");
        author = authorRepository.save(author);

        availableBook = new Book();
        availableBook.setTitle("Clean Code");
        availableBook.setIsbn("978-0132350884");
        availableBook.setPublicationYear(2008);
        availableBook.setAvailableCopies(5);
        availableBook.setCategory(category);
        availableBook.getAuthors().add(author);
        availableBook = bookRepository.save(availableBook);
        
        createUsers();
    }

    // ═══════════════════════════════════════════════════════════════════
    // CREATE LOAN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /api/loans - criar empréstimo")
    class CreateLoanTests {

        @Test
        @DisplayName("Deve criar empréstimo e associar ao usuário autenticado")
        void shouldCreateLoanForAuthenticatedUser() throws Exception {
            mockMvc.perform(asUser(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": [%d]}
                        """.formatted(availableBook.getId()))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("WAITING_RETURN"))
                .andExpect(jsonPath("$.userId").value(testUser.getName()));

            Book updated = bookRepository.findById(availableBook.getId()).orElseThrow();
            
            assertThat(updated.getAvailableCopies()).isEqualTo(4);
        }

        @Test
        @DisplayName("Deve retornar 409 quando livro não tem cópias disponíveis")
        void shouldReturn409WhenNotAvailable() throws Exception {
            availableBook.setAvailableCopies(0);
            bookRepository.save(availableBook);

            mockMvc.perform(asUser(post("/api/loans"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": [%d]}
                        """.formatted(availableBook.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Book Not Available"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando livro não existe")
        void shouldReturn404WhenBookNotFound() throws Exception {
            mockMvc.perform(asUser(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": [999]}
                        """)))
                .andExpect(status().isNotFound());
        }
        
        @Test
        @DisplayName("Deve retornar 400 quando booksId está vazio")
        void shouldReturn400WhenBooksIdEmpty() throws Exception {
            mockMvc.perform(asUser(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"booksId": []}
                        """)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Deve criar empréstimo com múltiplos livros")
        void shouldCreateLoanWithMultipleBooks() throws Exception {
            // Arrange - criar segundo livro
            Book book2 = new Book();
            book2.setTitle("Refactoring");
            book2.setIsbn("978-0134757599");
            book2.setPublicationYear(2018);
            book2.setAvailableCopies(3);
            book2.setCategory(category);
            book2.getAuthors().add(author);
            book2 = bookRepository.save(book2);

            String requestBody = """
                {
                    "booksId": [%d, %d]
                }
                """.formatted(availableBook.getId(), book2.getId());

            // Act & Assert
            mockMvc.perform(asUser(post("/api/loans")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.books").isArray())
                .andExpect(jsonPath("$.books").value(org.hamcrest.Matchers.hasSize(2)));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET LOAN BY ID
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/loans/{id} - buscar por ID")
    class GetLoanByIdTests {

        @Test
        @DisplayName("Usuário vê o próprio empréstimo")
        void shouldGetOwnLoan() throws Exception {
            Loan loan = createLoan(testUser, availableBook);

            mockMvc.perform(asUser(get("/api/loans/{id}", loan.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.getId()))
                .andExpect(jsonPath("$.userId").value(testUser.getName()));
        }

        @Test
        @DisplayName("Usuário NÃO vê empréstimo de outro — 404 intencional")
        void shouldReturn404WhenAccessingOtherUserLoan() throws Exception {
            Loan loan = createLoan(testUser, availableBook);

            // tokenForNewUser() cria um terceiro usuário sem ROLE_ADMIN
            String otherToken = tokenForNewUser("other@example.com", "ROLE_USER");

            mockMvc.perform(get("/api/loans/{id}", loan.getId())
                    .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN vê empréstimo de qualquer usuário")
        void adminShouldGetAnyLoan() throws Exception {
            Loan loan = createLoan(testUser, availableBook);

            mockMvc.perform(asAdmin(get("/api/loans/{id}", loan.getId())))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Usuário NÃO devolve empréstimo de outro — 404")
        void shouldReturn404WhenReturningOtherUserLoan() throws Exception {
            Loan loan = createLoan(testUser, availableBook);
            String otherToken = tokenForNewUser("other2@example.com", "ROLE_USER");

            mockMvc.perform(patch("/api/loans/{id}/return", loan.getId())
                    .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ADMIN devolve empréstimo de qualquer usuário")
        void adminShouldReturnAnyLoan() throws Exception {
            Loan loan = createLoan(testUser, availableBook);

            mockMvc.perform(asAdmin(patch("/api/loans/{id}/return", loan.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
        }

        @Test
        @DisplayName("Deve retornar 404 quando empréstimo não existe")
        void shouldReturn404WhenLoanNotFound() throws Exception {
            mockMvc.perform(asUser(get("/api/loans/{id}", 999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Loan Not Found"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GET MY LOANS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /api/loans/me - meus empréstimos")
    class GetMyLoansTests {

        @Test
        @DisplayName("Retorna apenas os empréstimos do usuário autenticado")
        void shouldReturnOnlyOwnLoans() throws Exception {
            createLoan(testUser, availableBook);

            Book book2 = new Book();
            book2.setTitle("Refactoring");
            book2.setIsbn("978-0134757599");
            book2.setAvailableCopies(3);
            book2.setCategory(categoryRepository.findAll().get(0));
            book2 = bookRepository.save(book2);
            createLoan(adminUser, book2);

            mockMvc.perform(asUser(get("/api/loans/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(testUser.getName()));
        }

        @Test
        @DisplayName("Retorna lista vazia quando não há empréstimos")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(asUser(get("/api/loans/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // RETURN LOAN
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PATCH /api/loans/{id}/return - devolver empréstimo")
    class ReturnLoanTests {

        @Test
        @DisplayName("Deve devolver empréstimo com sucesso")
        void shouldRestoreCopiesOnReturn() throws Exception {
            Loan loan = createLoan(testUser, availableBook);
            int copiesBefore = bookRepository.findById(availableBook.getId()).orElseThrow().getAvailableCopies();

            mockMvc.perform(asUser(patch("/api/loans/{id}/return", loan.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnDate").exists());

            int copiesAfter = bookRepository.findById(availableBook.getId()).orElseThrow().getAvailableCopies();
            assertThat(copiesAfter).isEqualTo(copiesBefore + 1);
        }

        @Test
        @DisplayName("Deve retornar 409 quando empréstimo já foi devolvido")
        void shouldReturn409WhenAlreadyReturned() throws Exception {
            Loan loan = createLoan(testUser, availableBook);
            loan.setStatus(LoanStatus.RETURNED);
            loan.setReturnDate(LocalDate.now());
            loanRepository.save(loan);

            mockMvc.perform(asUser(patch("/api/loans/{id}/return", loan.getId())))
                .andExpect(status().isConflict());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CANCEL LOAN
    // ═══════════════════════════════════════════════════════════════════

	@Nested
    @DisplayName("PATCH /api/loans/{id}/cancel - cancelar empréstimo")
    class CancelLoanTests {

        @Test
        @DisplayName("Deve cancelar e restaurar cópias do livro")
        void shouldRestoreCopiesOnCancel() throws Exception {
            Loan loan = createLoan(testUser, availableBook);
            int copiesBefore = bookRepository.findById(availableBook.getId()).orElseThrow().getAvailableCopies();

            mockMvc.perform(asUser(patch("/api/loans/{id}/cancel", loan.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

            int copiesAfter = bookRepository.findById(availableBook.getId()).orElseThrow().getAvailableCopies();
            assertThat(copiesAfter).isEqualTo(copiesBefore + 1);
        }
	}
	
    // ═══════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Endpoints restritos a ADMIN")
    class AdminEndpointsTests {

        @Test
        @DisplayName("GET /api/loans - ADMIN deve listar todos os empréstimos")
        void adminShouldListAll() throws Exception {
            createLoan(testUser, availableBook);

            mockMvc.perform(asAdmin(get("/api/loans")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("GET /api/loans - USER comum deve receber 403")
        void userShouldReceive403() throws Exception {
            mockMvc.perform(asUser(get("/api/loans")))
                .andExpect(status().isForbidden());
        }
        
        @Test
        @DisplayName("GET /api/loans/user/{userId} - ADMIN deve listar por usuário")
        void adminShouldListByUser() throws Exception {
            createLoan(testUser, availableBook);

            mockMvc.perform(asAdmin(get("/api/loans/user/{userId}", testUser.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(testUser.getName()));
        }
        
        @Test
        @DisplayName("GET /api/loans/overdue - ADMIN deve listar vencidos")
        void adminShouldListOverdue() throws Exception {
            Loan overdue = createLoan(testUser, availableBook);
            overdue.setDueDate(LocalDate.now().minusDays(1));
            overdue.setStatus(LoanStatus.WAITING_RETURN);
            loanRepository.save(overdue);

            mockMvc.perform(asAdmin(get("/api/loans/overdue")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════

    private Loan createLoan(User user, Book book) {
    	
    	Loan loan = new Loan();
        loan.setUser(user);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(7));
        loan.setStatus(LoanStatus.WAITING_RETURN);

        loanRepository.save(loan);
        
        LoanItemId id = new LoanItemId();
        id.setBookId(book.getId());
        id.setLoanId(loan.getId());
        loan.getItems().add(new LoanItem(id, loan, book, 1));
        
        // Decrementa cópias manualmente (já que não estamos passando pelo service)
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);
        

        return loanRepository.save(loan);
    }
}