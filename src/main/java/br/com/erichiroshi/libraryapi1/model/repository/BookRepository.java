package br.com.erichiroshi.libraryapi1.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.erichiroshi.libraryapi1.model.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
    boolean existsByIsbn(String isbn);
}
