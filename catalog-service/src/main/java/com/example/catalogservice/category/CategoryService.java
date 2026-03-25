package com.example.catalogservice.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.catalogservice.category.dto.CategoryCreateDTO;
import com.example.catalogservice.category.dto.CategoryResponseDTO;
import com.example.catalogservice.category.exception.CategoryAlreadyExistsException;
import com.example.catalogservice.category.exception.CategoryNotFoundException;
import com.example.catalogservice.category.mapper.CategoryMapper;
import com.example.catalogservice.common.dto.PageResponseDTO;

@Service
public class CategoryService {

	private final CategoryRepository repository;
	private final CategoryMapper mapper;

	public CategoryService(CategoryRepository repository, CategoryMapper mapper) {
		this.repository = repository;
		this.mapper = mapper;
	}

	@Transactional
	public CategoryResponseDTO create(CategoryCreateDTO dto) {
		Optional<Category> entity = repository.findByNameIgnoreCase(dto.name());

		if (entity.isPresent()) {
			throw new CategoryAlreadyExistsException(dto.name());
		}

		Category category = mapper.toEntity(dto);
		category = repository.save(category);
		
		return mapper.toDTO(category);
	}

	@Transactional(readOnly = true)
	public CategoryResponseDTO findById(Long id) {
		return mapper.toDTO(find(id));
	}

	@Transactional(readOnly = true)
	public PageResponseDTO<CategoryResponseDTO> findAll(Pageable pageable) {
	    Page<Category> page = repository.findAll(pageable);
	    
	    List<CategoryResponseDTO> content = page.getContent()
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

	private Category find(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new CategoryNotFoundException(id));
	}
}
