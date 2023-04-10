package br.com.erichiroshi.libraryapi1.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.erichiroshi.libraryapi1.model.entity.Book;
import br.com.erichiroshi.libraryapi1.model.entity.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long> {

	@Query("SELECT CASE WHEN (COUNT(l.id) > 0) THEN true ELSE false END "
			+ "FROM Loan l WHERE l.book = :book AND (l.returned IS NULL OR l.returned = false)")
	boolean existsByBookAndNotReturned(@Param("book") Book book);

}
