package com.example.library.author.application;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.author.application.dto.AuthorCreateDTO;
import com.example.library.author.application.dto.AuthorResponseDTO;
import com.example.library.author.application.dto.PageResponseDTO;
import com.example.library.author.domain.Author;
import com.example.library.author.domain.AuthorRepository;
import com.example.library.author.exception.AuthorNotFoundException;
import com.example.library.author.mapper.AuthorMapper;

@Service
public class AuthorService {

	private final AuthorRepository repository;
	private final AuthorMapper mapper;

	public AuthorService(AuthorRepository repository, AuthorMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Transactional
	public AuthorResponseDTO create(AuthorCreateDTO dto) {
		Author author = repository.save(mapper.toEntity(dto));
		return mapper.toDTO(author);
	}

	@Transactional(readOnly = true)
	public AuthorResponseDTO findById(Long id) {
		Author author = find(id);
		return mapper.toDTO(author);

	}

	@Transactional(readOnly = true)
	public PageResponseDTO<AuthorResponseDTO> findAll(Pageable pageable) {

		Page<Author> page = repository.findAll(pageable);

		List<AuthorResponseDTO> content =
	            page.getContent()
	                .stream()
	                .map(mapper::toDTO)
	                .toList();

	    return new PageResponseDTO<>(
	            content,
	            page.getNumber(),
	            page.getSize(),
	            page.getTotalElements(),
	            page.getTotalPages()
	    );
	}

	@Transactional
	public void deleteById(Long id) {
		find(id);
		repository.deleteById(id);
	}

	private Author find(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new AuthorNotFoundException(id));
	}
}
