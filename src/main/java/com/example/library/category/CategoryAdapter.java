package com.example.library.category;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryAdapter implements CategoryPort {

	private final CategoryRepository repository;

	@Override
	@Transactional(readOnly = true)
	public Optional<Category> findById(Long id) {
		return repository.findById(id);
	}
}