package com.example.catalogservice.book.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.catalogservice.author.Author;
import com.example.catalogservice.book.Book;
import com.example.catalogservice.book.dto.BookCreateDTO;
import com.example.catalogservice.book.dto.BookResponseDTO;

@Mapper(componentModel = "spring")
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
