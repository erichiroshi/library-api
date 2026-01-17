package com.example.library.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.library.api.dto.request.CategoryRequestDTO;
import com.example.library.api.dto.response.CategoryResponseDTO;
import com.example.library.domain.entities.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

	CategoryResponseDTO toDTO(Category entity);

	@Mapping(target = "id", ignore = true)
	Category toEntity(CategoryRequestDTO dto);
}
