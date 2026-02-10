package com.example.library.category;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.library.category.dto.CategoryRequestDTO;
import com.example.library.category.dto.CategoryResponseDTO;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

	CategoryResponseDTO toDTO(Category entity);

	@Mapping(target = "id", ignore = true)
	Category toEntity(CategoryRequestDTO dto);
}
