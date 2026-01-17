package com.example.library.domain.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.api.dto.request.CategoryRequestDTO;
import com.example.library.api.dto.response.CategoryResponseDTO;
import com.example.library.api.mapper.CategoryMapper;
import com.example.library.domain.entities.Category;
import com.example.library.domain.repositories.CategoryRepository;

@Service
public class CategoryService {

	private final CategoryRepository repository;
	private final CategoryMapper mapper;

	public CategoryService(CategoryRepository repository, CategoryMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Transactional
	public CategoryResponseDTO create(CategoryRequestDTO dto) {

		if (repository.existsByNameIgnoreCase(dto.name())) {
			System.out.println("Category with name " + dto.name() + " already exists.");
		}

		Category category = mapper.toEntity(dto);

		Category saved = repository.save(category);
		return mapper.toDTO(saved);
	}

	@Transactional(readOnly = true)
	public CategoryResponseDTO findById(Long id) {
		Category category = repository.findById(id).get();
		return mapper.toDTO(category);
	}

	@Transactional(readOnly = true)
	public List<CategoryResponseDTO> findAll() {
		return repository.findAll()
				.stream()
				.map(mapper::toDTO)
				.toList();
	}
}
