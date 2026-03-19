package com.example.library.author.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.library.author.Author;
import com.example.library.author.dto.AuthorCreateDTO;
import com.example.library.author.dto.AuthorResponseDTO;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

	AuthorResponseDTO toDTO(Author author);

	@Mapping(target = "id", ignore = true)
	Author toEntity(AuthorCreateDTO dto);
}
