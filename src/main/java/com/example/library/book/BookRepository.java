package com.example.library.book;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

	List<Book> findByTitleContainingIgnoreCase(String title);

	@Query("""
		    SELECT b FROM Book b
		    JOIN b.category c
		    WHERE c.name = :category
		""")
		List<Book> findByCategoryName(String category);

    boolean existsByIsbn(String isbn);
    
	@Modifying
	@Query("""

			UPDATE Book b
			SET b.availableCopies = b.availableCopies - 1
			WHERE b.id = :id
			AND b.availableCopies > 0""")
	int decrementCopies(@Param("id") Long id);

}
