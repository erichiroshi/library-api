package com.example.catalogservice.author.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.catalogservice.author.Author;
import com.example.catalogservice.author.dto.AuthorCreateDTO;
import com.example.catalogservice.author.dto.AuthorResponseDTO;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

	AuthorResponseDTO toDTO(Author author);

	@Mapping(target = "id", ignore = true)
	Author toEntity(AuthorCreateDTO dto);
}
