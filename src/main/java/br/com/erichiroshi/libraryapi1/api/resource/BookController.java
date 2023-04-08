package br.com.erichiroshi.libraryapi1.api.resource;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

import br.com.erichiroshi.libraryapi1.api.dto.BookDTO;
import br.com.erichiroshi.libraryapi1.api.resource.exception.LivroNaoExisteException;
import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.service.BookService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books")
public class BookController {

	@Autowired
	private BookService service;
	@Autowired
	private ModelMapper modelMapper;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookDTO create(@RequestBody @Valid BookDTO bookDTO) {
		Book entity = modelMapper.map(bookDTO, Book.class);
		entity = service.save(entity);
		return modelMapper.map(entity, BookDTO.class);
	}

	@GetMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public BookDTO findById(@PathVariable Long id) {
		Book book = service.getById(id).orElseThrow(() -> new LivroNaoExisteException("Livro não encontrado."));
		return modelMapper.map(book, BookDTO.class);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteById(@PathVariable Long id) {
		Book book = service.getById(id).orElseThrow(() -> new LivroNaoExisteException("Livro não encontrado."));
		service.delete(book);
	}

	@PutMapping("/{id}")
	@ResponseStatus(HttpStatus.OK)
	public BookDTO update(@RequestBody BookDTO bookDto, @PathVariable Long id) {
		Book book = service.getById(id).orElseThrow(() -> new LivroNaoExisteException("Livro não encontrado."));
		book.setAuthor(bookDto.getAuthor());
		book.setTitle(bookDto.getTitle());
		service.update(book);
		return modelMapper.map(book, BookDTO.class);
	}
	
	@GetMapping
	public Page<BookDTO> find(BookDTO dto, Pageable pageRequest) {
		Book filter = modelMapper.map(dto, Book.class);
		Page<Book> result = service.find(filter, pageRequest);
		List<BookDTO> list = result.getContent().stream()
				.map(entity -> modelMapper.map(entity, BookDTO.class))
				.collect(Collectors.toList());

		return new PageImpl<BookDTO>(list, pageRequest, result.getTotalElements());
	}
}
