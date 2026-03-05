package com.example.library.author;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorLookupServiceImpl - Unit Tests")
class AuthorLookupServiceImplTest {

    @Mock
    private AuthorRepository repository;

    @InjectMocks
    private AuthorLookupServiceImpl service;

    @Test
    @DisplayName("should return all authors by ids")
    void shouldReturnAllAuthorsByIds() {
        Author a1 = new Author();
        a1.setId(1L);
        Author a2 = new Author();
        a2.setId(2L);
        List<Long> ids = List.of(1L, 2L);

        when(repository.findAllById(ids)).thenReturn(List.of(a1, a2));

        Set<Author> result = service.findAllById(ids);

        assertThat(result).containsExactlyInAnyOrder(a1, a2);
    }

    @Test
    @DisplayName("should return empty set when no authors found")
    void shouldReturnEmptySetWhenNoAuthorsFound() {
        List<Long> ids = List.of(99L);

        when(repository.findAllById(ids)).thenReturn(List.of());

        Set<Author> result = service.findAllById(ids);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return correct count by ids")
    void shouldReturnCorrectCountByIds() {
        Author a1 = new Author();
        a1.setId(1L);
        Author a2 = new Author();
        a2.setId(2L);
        List<Long> ids = List.of(1L, 2L);

        when(repository.findAllById(ids)).thenReturn(List.of(a1, a2));

        int count = service.countByIds(ids);

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("should return zero when no authors match ids")
    void shouldReturnZeroWhenNoAuthorsMatchIds() {
        List<Long> ids = List.of(99L);

        when(repository.findAllById(ids)).thenReturn(List.of());

        int count = service.countByIds(ids);

        assertThat(count).isZero();
    }
}