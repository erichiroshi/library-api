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
    
    /**
     * Decrementa atomicamente as cópias disponíveis.
     * Retorna 0 se não havia cópias (availableCopies já era 0) — sem UPDATE executado.
     *
     * clearAutomatically = true: após o UPDATE, o cache de primeiro nível do JPA
     * é limpo. Sem isso, chamadas a findById() na mesma transação retornariam
     * o valor antigo em memória, ignorando o UPDATE executado no banco.
     *
     * flushAutomatically = true: garante que operações pendentes (inserts/updates
     * via dirty checking) são enviadas ao banco ANTES do UPDATE, evitando
     * inconsistências de ordem de execução.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)  // ← limpa o cache após o UPDATE
	@Query("""

			UPDATE Book b
			SET b.availableCopies = b.availableCopies - 1
			WHERE b.id = :id
			AND b.availableCopies > 0""")
	int decrementCopies(@Param("id") Long id);

}
