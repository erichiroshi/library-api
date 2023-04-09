package br.com.erichiroshi.libraryapi1.api.resource;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.erichiroshi.libraryapi1.api.dto.LoanDTO;
import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.model.entity.Loan;
import br.com.erichiroshi.libraryapi1.service.BookService;
import br.com.erichiroshi.libraryapi1.service.LoanService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService service;
    private final BookService bookService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long create( @RequestBody LoanDTO dto ){
        Book book = bookService.getBookByIsbn(dto.getIsbn())
        				.orElseThrow( () ->new ResponseStatusException( HttpStatus.BAD_REQUEST, "Book not found for passed isbn"));
        
        Loan entity = Loan.builder()
                    .book(book)
                    .customer(dto.getCustomer())
                    .loanDate(LocalDate.now())
                    .build();

        entity = service.save(entity);
        return entity.getId();
    }
}