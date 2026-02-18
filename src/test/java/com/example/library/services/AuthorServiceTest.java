package com.example.library.services;

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

import com.example.library.author.Author;
import com.example.library.author.AuthorRepository;
import com.example.library.author.AuthorService;
import com.example.library.author.dto.AuthorCreateDTO;
import com.example.library.author.dto.AuthorResponseDTO;
import com.example.library.author.dto.PageResponseDTO;
import com.example.library.author.exception.AuthorNotFoundException;
import com.example.library.author.mapper.AuthorMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorService - Unit Tests")
class AuthorServiceTest {

    @Mock
    private AuthorRepository repository;

    @Mock
    private AuthorMapper mapper;

    @InjectMocks
    private AuthorService authorService;

    private Author author;
    private AuthorCreateDTO createDTO;
    private AuthorResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        author = new Author();
        author.setId(1L);
        author.setName("Robert C. Martin");
        author.setBiography("Autor e engenheiro de software");

        createDTO = new AuthorCreateDTO("Robert C. Martin", "Autor e engenheiro de software");
        responseDTO = new AuthorResponseDTO(1L, "Robert C. Martin", "Autor e engenheiro de software");
    }	

    @Nested
    @DisplayName("create() - criar autor")
    class CreateTests {

        @Test
        @DisplayName("Deve criar autor com sucesso")
        void shouldCreateAuthor() {
            // Arrange
            when(mapper.toEntity(createDTO)).thenReturn(author);
            when(repository.save(author)).thenReturn(author);
            when(mapper.toDTO(author)).thenReturn(responseDTO);

            // Act
            AuthorResponseDTO result = authorService.create(createDTO);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Robert C. Martin");
            verify(repository).save(author);
        }

        @Test
        @DisplayName("Deve criar autor sem data de nascimento")
        void shouldCreateAuthorWithoutBiography() {
            // Arrange
            AuthorCreateDTO dtoWithoutBiography = new AuthorCreateDTO("John Doe", null);
            Author authorWithoutBiography = new Author();
            authorWithoutBiography.setId(2L);
            authorWithoutBiography.setName("John Doe");

            when(mapper.toEntity(dtoWithoutBiography)).thenReturn(authorWithoutBiography);
            when(repository.save(authorWithoutBiography)).thenReturn(authorWithoutBiography);
            when(mapper.toDTO(authorWithoutBiography))
                .thenReturn(new AuthorResponseDTO(2L, "John Doe", null));

            // Act
            AuthorResponseDTO result = authorService.create(dtoWithoutBiography);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.biography()).isNull();
        }
    }

    @Nested
    @DisplayName("findById() - buscar por ID")
    class FindByIdTests {

        @Test
        @DisplayName("Deve retornar autor quando existe")
        void shouldReturnAuthor() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(author));
            when(mapper.toDTO(author)).thenReturn(responseDTO);

            // Act
            AuthorResponseDTO result = authorService.findById(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Robert C. Martin");
            assertThat(result.biography()).isEqualTo("Autor e engenheiro de software");
        }

        @Test
        @DisplayName("Deve lançar AuthorNotFoundException quando não existe")
        void shouldThrowNotFoundException() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authorService.findById(999L))
                .isInstanceOf(AuthorNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll() - listar com paginação")
    class FindAllTests {

        @Test
        @DisplayName("Deve retornar página de autores")
        void shouldReturnPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Author> page = new PageImpl<>(List.of(author), pageable, 1);

            when(repository.findAll(pageable)).thenReturn(page);
            when(mapper.toDTO(author)).thenReturn(responseDTO);

            // Act
            PageResponseDTO<AuthorResponseDTO> result = authorService.findAll(pageable);

            // Assert
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content().get(0).name()).isEqualTo("Robert C. Martin");
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não há autores")
        void shouldReturnEmptyPage() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Author> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            when(repository.findAll(pageable)).thenReturn(emptyPage);

            // Act
            PageResponseDTO<AuthorResponseDTO> result = authorService.findAll(pageable);

            // Assert
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("deleteById() - deletar")
    class DeleteTests {

        @Test
        @DisplayName("Deve deletar autor com sucesso")
        void shouldDelete() {
            // Arrange
            when(repository.findById(1L)).thenReturn(Optional.of(author));

            // Act
            authorService.deleteById(1L);

            // Assert
            verify(repository).findById(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar AuthorNotFoundException ao deletar inexistente")
        void shouldThrowNotFoundWhenDeleting() {
            // Arrange
            when(repository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authorService.deleteById(999L))
                .isInstanceOf(AuthorNotFoundException.class);

            verify(repository, never()).deleteById(anyLong());
        }
    }
}