//package com.example.loanservice.controller;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
//import org.springframework.http.MediaType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.transaction.annotation.Transactional;
//import org.testcontainers.junit.jupiter.Testcontainers;
//
//import com.example.loanservice.loan.LoanRepository;
//import com.github.tomakehurst.wiremock.client.WireMock;
//import com.github.tomakehurst.wiremock.junit5.WireMockTest;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
//import static com.github.tomakehurst.wiremock.client.WireMock.get;
//import static com.github.tomakehurst.wiremock.client.WireMock.patch;
//import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@AutoConfigureMockMvc
//@WireMockTest(httpPort = 0)
//@Testcontainers
//@Transactional
//@ActiveProfiles("it")
//@TestPropertySource(properties = {
//	    // Feign Clients apontam para o WireMock
//	    "spring.cloud.openfeign.client.config.catalog-service.url=http://localhost:${wiremock.server.port}",
//	    "spring.cloud.openfeign.client.config.auth-service.url=http://localhost:${wiremock.server.port}"
//	})
//@DisplayName("LoanController - Integration Tests (Microservice)")
//class LoanControllerIT {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private LoanRepository loanRepository;
//
//    private static final String USER_EMAIL = "john@example.com";
//    private static final Long BOOK_ID = 1L;
//    
//    @BeforeEach
//    void setUp() {
//        WireMock.reset();
//
//        // Stub: auth-service — findByEmail
//        stubFor(get(urlEqualTo("/internal/users/by-email?email=" + USER_EMAIL))
//            .willReturn(aResponse()
//                .withHeader("Content-Type", "application/json")
//                .withBody("""
//                    {
//                        "id": 1,
//                        "name": "John Doe",
//                        "email": "%s",
//                        "roles": ["ROLE_USER"]
//                    }
//                    """.formatted(USER_EMAIL))));
//
//        // Stub: catalog-service — findById
//        stubFor(get(urlEqualTo("/api/v1/books/" + BOOK_ID))
//            .willReturn(aResponse()
//                .withHeader("Content-Type", "application/json")
//                .withBody("""
//                    {
//                        "id": %d,
//                        "title": "Clean Code",
//                        "availableCopies": 5
//                    }
//                    """.formatted(BOOK_ID))));
//
//        // Stub: catalog-service — decrementCopies
//        stubFor(patch(urlEqualTo("/api/v1/books/" + BOOK_ID + "/decrement"))
//            .willReturn(aResponse()
//                .withHeader("Content-Type", "application/json")
//                .withBody("1")));  // retorna 1 — sucesso
//    }
//
//    @Nested
//    @DisplayName("POST /api/v1/loans - criar empréstimo")
//    class CreateLoanTests {
//
//        @Test
//        @DisplayName("Deve criar empréstimo com sucesso")
//        void shouldCreateLoan() throws Exception {
//            mockMvc.perform(post("/api/v1/loans")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .header("X-User-Id", USER_EMAIL)
//                    .content("""
//                        {"booksId": [%d]}
//                        """.formatted(BOOK_ID)))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.status").value("WAITING_RETURN"))
//                .andExpect(jsonPath("$.userId").exists());
//
//            assertThat(loanRepository.count()).isEqualTo(1);
//        }
//
//        @Test
//        @DisplayName("Deve retornar 409 quando livro sem cópias")
//        void shouldReturn409WhenNoCopies() throws Exception {
//            // Sobrescreve stub — decrement retorna 0
//            stubFor(patch(urlEqualTo("/api/v1/books/" + BOOK_ID + "/decrement"))
//                .willReturn(aResponse()
//                    .withHeader("Content-Type", "application/json")
//                    .withBody("0")));
//
//            mockMvc.perform(post("/api/v1/loans")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .header("X-User-Id", USER_EMAIL)
//                    .content("""
//                        {"booksId": [%d]}
//                        """.formatted(BOOK_ID)))
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.title").value("Book Not Available"));
//        }
//
//        @Test
//        @DisplayName("Deve retornar 400 quando booksId está vazio")
//        void shouldReturn400WhenBooksIdEmpty() throws Exception {
//            mockMvc.perform(post("/api/v1/loans")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .header("X-User-Id", USER_EMAIL)
//                    .content("""
//                        {"booksId": []}
//                        """))
//                .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @DisplayName("Deve retornar 404 quando livro não existe no catalog-service")
//        void shouldReturn404WhenBookNotFound() throws Exception {
//            stubFor(get(urlEqualTo("/api/v1/books/999"))
//                .willReturn(aResponse().withStatus(404)));
//
//            mockMvc.perform(post("/api/v1/loans")
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .header("X-User-Id", USER_EMAIL)
//                    .content("""
//                        {"booksId": [999]}
//                        """))
//                .andExpect(status().isNotFound());
//        }
//    }
//
//    @Nested
//    @DisplayName("GET /api/v1/loans/me - meus empréstimos")
//    class GetMyLoansTests {
//
//        @Test
//        @DisplayName("Deve retornar lista vazia quando sem empréstimos")
//        void shouldReturnEmptyList() throws Exception {
//            mockMvc.perform(
//                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders
//                        .get("/api/v1/loans/me")
//                        .header("X-User-Id", USER_EMAIL))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$").isEmpty());
//        }
//    }
//}