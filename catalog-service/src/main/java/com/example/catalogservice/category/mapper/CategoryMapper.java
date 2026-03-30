package com.example.catalogservice.category.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.catalogservice.category.Category;
import com.example.catalogservice.category.dto.CategoryCreateDTO;
import com.example.catalogservice.category.dto.CategoryResponseDTO;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

	CategoryResponseDTO toDTO(Category entity);

	@Mapping(target = "id", ignore = true)
	Category toEntity(CategoryCreateDTO dto);
}
