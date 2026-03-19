package com.example.library.book.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import com.example.library.author.Author;
import com.example.library.book.Book;
import com.example.library.book.dto.BookCreateDTO;
import com.example.library.book.dto.BookResponseDTO;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookMapper {

	@Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "authors", target = "authorIds")
	BookResponseDTO toDTO(Book book);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "authors", ignore = true)
    @Mapping(target = "coverImageUrl", ignore = true)
	Book toEntity(BookCreateDTO dto);
    
    default Set<Long> mapAuthors(Set<Author> authors) {
        return authors.stream()
                      .map(Author::getId)
                      .collect(Collectors.toSet());
    }
}
