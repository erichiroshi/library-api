package com.example.library.author;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.library.author.dto.AuthorRequestDTO;
import com.example.library.author.dto.AuthorResponseDTO;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

	AuthorResponseDTO toDTO(Author author);

	@Mapping(target = "id", ignore = true)
	Author toEntity(AuthorRequestDTO dto);
}
