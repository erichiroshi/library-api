package br.com.erichiroshi.libraryapi1.api.resource;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.erichiroshi.libraryapi1.api.dto.BookDTO;
import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.service.BookService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books")
public class BookController {

	@Autowired
	private BookService service;
	@Autowired
	private ModelMapper mapper;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookDTO create(@RequestBody @Valid BookDTO bookDTO) {
		Book entity = mapper.map(bookDTO, Book.class);
		entity = service.save(entity);
		return mapper.map(entity, BookDTO.class);
	}
}
