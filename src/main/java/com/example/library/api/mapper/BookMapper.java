package com.example.library.api.mapper;

import org.mapstruct.Mapper;

import com.example.library.api.dto.request.BookRequestDTO;
import com.example.library.domain.entities.Book;

@Mapper(componentModel = "spring")
public interface BookMapper {

	BookRequestDTO toDTO(Book book);

	Book toEntity(BookRequestDTO dto);
}
