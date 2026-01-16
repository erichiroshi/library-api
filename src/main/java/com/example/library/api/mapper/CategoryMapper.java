package com.example.library.api.mapper;

import com.example.library.api.dto.request.CategoryRequestDTO;
import com.example.library.api.dto.response.CategoryResponseDTO;
import com.example.library.domain.entities.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

	Category toEntity(CategoryRequestDTO dto);

	CategoryResponseDTO toDTO(Category entity);
}
