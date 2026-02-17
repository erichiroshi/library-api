package com.example.library.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.library.category.Category;
import com.example.library.category.CategoryRepository;
import com.example.library.category.CategoryService;
import com.example.library.category.dto.CategoryCreateDTO;
import com.example.library.category.dto.CategoryResponseDTO;
import com.example.library.category.dto.PageResponseDTO;
import com.example.library.category.exception.CategoryAlreadyExistsException;
import com.example.library.category.exception.CategoryNotFoundException;
import com.example.library.category.mapper.CategoryMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService - Unit Tests")
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @Mock
    private CategoryMapper mapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryCreateDTO createDTO;
    private CategoryResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Technology");

        createDTO = new CategoryCreateDTO("Technology");
        responseDTO = new CategoryResponseDTO(1L, "Technology");
    }

    @Nested
    @DisplayName("create() - criar categoria")
    class CreateTests {

        @Test
        @DisplayName("Deve criar categoria com sucesso")
        void shouldCreateCategory() {
            // Arrange
            when(repository.findByNameIgnoreCase("Technology")).thenReturn(Optional.empty());
            when(mapper.toEntity(createDTO)).thenReturn(category);
            when(repository.save(category)).thenReturn(category);
            when(mapper.toDTO(category)).thenReturn(responseDTO);

            // Act
            CategoryResponseDTO result = categoryService.create(createDTO);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Technology");
            verify(repository).save(category);
        }

        @Test
        @DisplayName("Deve lançar CategoryAlreadyExistsException quando nome já existe")
        void shouldThrowCategoryAlreadyExists() {
            // Arrange
            when(repository.findByNameIgnoreCase("Technology")).thenReturn(Optional.of(category));

            // Act & Assert
            assertThatThrownBy(() -> categoryService.create(createDTO))
                .isInstanceOf(CategoryAlreadyExistsException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve ser case-insensitive ao verificar duplicação")
        void shouldBeCaseInsensitiveWhenChecking() {
            // Arrange
            when(repository.findByNameIgnoreCase("TECHNOLOGY")).thenReturn(Optional.of(category));
            CategoryCreateDTO dtoUppercase = new CategoryCreateDTO("TECHNOLOGY");

            // Act & Assert
            assertThatThrownBy(() -> categoryService.create(dtoUppercase))
                .isInstanceOf(CategoryAlreadyExistsException.class);
        }
    }

    @Nested
    @DisplayName("findById() - buscar por ID")
    class FindByIdTests {

        @Test
        @DisplayName("Deve retornar categoria quando existe")
        void shouldReturnCategory() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(category));
            when(mapper.toDTO(category)).thenReturn(responseDTO);

            // Act
            CategoryResponseDTO result = categoryService.findById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Technology");
        }

        @Test
        @DisplayName("Deve lançar CategoryNotFoundException quando não existe")
        void shouldThrowNotFoundException() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.findById(999L))
                .isInstanceOf(CategoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll() - listar com paginação")
    class FindAllTests {

        @Test
        @DisplayName("Deve retornar página de categorias")
        void shouldReturnPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Category> page = new PageImpl<>(List.of(category), pageable, 1);

            when(repository.findAll(pageable)).thenReturn(page);
            when(mapper.toDTO(category)).thenReturn(responseDTO);

            // Act
            PageResponseDTO<CategoryResponseDTO> result = categoryService.findAll(pageable);

            // Assert
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("deleteById() - deletar")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar categoria com sucesso")
        void shouldDelete() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(category));

            // Act
            categoryService.deleteById(1L);

            // Assert
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar CategoryNotFoundException ao deletar inexistente")
        void shouldThrowNotFoundWhenDeleting() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteById(999L))
                .isInstanceOf(CategoryNotFoundException.class);

            verify(repository, never()).deleteById(anyLong());
        }
    }
}