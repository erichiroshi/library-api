package com.example.library.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.library.api.dto.request.AuthorRequestDTO;
import com.example.library.api.dto.response.AuthorResponseDTO;
import com.example.library.domain.entities.Author;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

	AuthorResponseDTO toDTO(Author author);

	@Mapping(target = "id", ignore = true)
	Author toEntity(AuthorRequestDTO dto);
}
