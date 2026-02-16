package com.example.library.loan;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {

	/**
	 * Todos os empréstimos de um usuário específico.
	 */
	List<Loan> findByUserId(Long userId);

	/**
	 * Empréstimos vencidos: status WAITING_RETURN e dueDate antes de hoje.
	 */
	@Query("""
			SELECT l FROM Loan l
			WHERE l.status = com.example.library.loan.LoanStatus.WAITING_RETURN
			AND l.dueDate < :today
			""")
	List<Loan> findOverdueLoans(@Param("today") LocalDate today);

	/**
	 * Empréstimos ativos de um usuário (para verificar limite de empréstimos).
	 */
	@Query("""
			SELECT COUNT(l) FROM Loan l
			WHERE l.user.id = :userId
			AND l.status = com.example.library.loan.LoanStatus.WAITING_RETURN
			""")
	long countActiveByUserId(@Param("userId") Long userId);
}