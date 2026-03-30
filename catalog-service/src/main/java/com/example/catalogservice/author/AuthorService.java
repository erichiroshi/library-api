package com.example.catalogservice.author;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.catalogservice.author.dto.AuthorCreateDTO;
import com.example.catalogservice.author.dto.AuthorResponseDTO;
import com.example.catalogservice.author.exception.AuthorNotFoundException;
import com.example.catalogservice.author.mapper.AuthorMapper;
import com.example.catalogservice.common.dto.PageResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorService {

	private final AuthorRepository repository;
	private final AuthorMapper mapper;

	@Transactional
	public AuthorResponseDTO create(AuthorCreateDTO dto) {
		
		Author author = repository.save(mapper.toEntity(dto));
		return mapper.toDTO(author);
	}

	@Transactional(readOnly = true)
	public AuthorResponseDTO findById(Long id) {
		return mapper.toDTO(find(id));
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
