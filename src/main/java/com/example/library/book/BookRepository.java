package com.example.library.book;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Long> {

	List<Book> findByTitleContainingIgnoreCase(String title);

	@Query("""
		    SELECT b FROM Book b
		    JOIN b.category c
		    WHERE c.name = :category
		""")
		List<Book> findByCategoryName(String category);

    boolean existsByIsbn(String isbn);

}
