package com.example.library.domain.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.library.domain.entities.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

	List<Book> findByTitleContainingIgnoreCase(String title);

	@Query("""
		    SELECT b FROM Book b
		    JOIN b.category c
		    WHERE c.name = :category
		""")
		List<Book> findByCategoryName(String category);

}
