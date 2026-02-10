package com.example.library.author;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.author.dto.AuthorRequestDTO;
import com.example.library.author.dto.AuthorResponseDTO;

@Service
public class AuthorService {

	private final AuthorRepository repository;
	private final AuthorMapper mapper;

	public AuthorService(AuthorRepository repository, AuthorMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Transactional
	public AuthorResponseDTO create(AuthorRequestDTO dto) {
		Author saved = repository.save(mapper.toEntity(dto));
		return mapper.toDTO(saved);
	}

	@Transactional(readOnly = true)
	public AuthorResponseDTO findById(Long id) {
		Author author = repository.findById(id).get();
		return mapper.toDTO(author);

	}

	@Transactional(readOnly = true)
	public List<AuthorResponseDTO> findAll() {
		return repository.findAll()
				.stream()
				.map(mapper::toDTO)
				.toList();
	}
}
