package com.example.library.category;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryLookupServiceImpl - Unit Tests")
class CategoryLookupServiceImplTest {

    @Mock
    private CategoryRepository repository;

    @InjectMocks
    private CategoryLookupServiceImpl service;

    @Test
    @DisplayName("should return category when found by id")
    void shouldReturnCategoryWhenFoundById() {
        Category category = new Category();
        when(repository.findById(1L)).thenReturn(Optional.of(category));

        Optional<Category> result = service.findById(1L);

        assertThat(result).isPresent().contains(category);
    }

    @Test
    @DisplayName("should return empty when category not found by id")
    void shouldReturnEmptyWhenCategoryNotFoundById() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<Category> result = service.findById(99L);

        assertThat(result).isEmpty();
    }
}