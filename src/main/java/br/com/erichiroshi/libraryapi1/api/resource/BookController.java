package br.com.erichiroshi.libraryapi1.api.resource;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.erichiroshi.libraryapi1.api.dto.BookDTO;
import br.com.erichiroshi.libraryapi1.api.dto.LoanDTO;
import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.model.entity.Loan;
import br.com.erichiroshi.libraryapi1.service.BookService;
import br.com.erichiroshi.libraryapi1.service.LoanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Api("Book API")
public class BookController {

	private final BookService service;
	private final ModelMapper modelMapper;
	private final LoanService loanService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
    @ApiOperation("Create a book")
	public BookDTO create(@RequestBody @Valid BookDTO dto) {
        Book entity = modelMapper.map( dto, Book.class );
        entity = service.save(entity);
        return modelMapper.map(entity, BookDTO.class);
    }

    @GetMapping("{id}")
    @ApiOperation("Create a book")
    public BookDTO get( @PathVariable Long id ){
        return service
                .getById(id)
                .map( book -> modelMapper.map(book, BookDTO.class)  )
                .orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND) );
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiOperation("Deletes a book by id")
    public void delete(@PathVariable Long id){
        Book book = service.getById(id).orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND) );
        service.delete(book);
    }

    @PutMapping("{id}")
    @ApiOperation("Updates a book")
    public BookDTO update( @PathVariable Long id, BookDTO dto){
        return service.getById(id).map( book -> {

            book.setAuthor(dto.getAuthor());
            book.setTitle(dto.getTitle());
            book = service.update(book);
            return modelMapper.map(book, BookDTO.class);

        }).orElseThrow( () -> new ResponseStatusException(HttpStatus.NOT_FOUND) );
    }

    @GetMapping
    @ApiOperation("Lists books by params")
    public Page<BookDTO> find( BookDTO dto, Pageable pageRequest ){
        Book filter = modelMapper.map(dto, Book.class);
        Page<Book> result = service.find(filter, pageRequest);
        List<BookDTO> list = result.getContent()
                .stream()
                .map(entity -> modelMapper.map(entity, BookDTO.class))
                .collect(Collectors.toList());

        return new PageImpl<BookDTO>( list, pageRequest, result.getTotalElements() );
    }

	@GetMapping("{id}/loans")
	public Page<LoanDTO> loansByBook(@PathVariable Long id, Pageable pageable) {
		Book book = service.getById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		Page<Loan> result = loanService.getLoansByBook(book, pageable);
		List<LoanDTO> list = result.getContent()
				.stream()
                .map(loan -> {
                    Book loanBook = loan.getBook();
                    BookDTO bookDTO = modelMapper.map(loanBook, BookDTO.class);
                    LoanDTO loanDTO = modelMapper.map(loan, LoanDTO.class);
                    loanDTO.setBook(bookDTO);
                    return loanDTO;
                }).collect(Collectors.toList());
		
        return new PageImpl<LoanDTO>(list, pageable, result.getTotalElements());
    }

}
