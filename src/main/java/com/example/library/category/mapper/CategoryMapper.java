package com.example.library.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.library.category.application.dto.CategoryCreateDTO;
import com.example.library.category.application.dto.CategoryResponseDTO;
import com.example.library.category.domain.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

	CategoryResponseDTO toDTO(Category entity);

	@Mapping(target = "id", ignore = true)
	Category toEntity(CategoryCreateDTO dto);
}
