package com.example.library.loan;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {
	
	 /**
     * Busca um empréstimo pelo ID carregando itens e usuário via JOIN FETCH.
     * Necessário após migração de EAGER para LAZY para evitar LazyInitializationException
     * ao acessar items e user fora da sessão JPA.
     */
    @Query("""
            SELECT l FROM Loan l
            JOIN FETCH l.items i
            JOIN FETCH i.book
            JOIN FETCH l.user
            WHERE l.id = :id
            """)
    Optional<Loan> findByIdWithItemsAndUser(@Param("id") Long id);
    
    /**
     * Todos os empréstimos de um usuário específico, com itens e livros carregados.
     */
    @Query("""
            SELECT DISTINCT l FROM Loan l
            JOIN FETCH l.items i
            JOIN FETCH i.book
            WHERE l.user.id = :userId
            """)
    List<Loan> findByUserIdWithItems(@Param("userId") Long userId);

	 /**
     * Empréstimos vencidos: status WAITING_RETURN e dueDate antes de hoje.
     * Não precisa de itens — apenas marca o status.
     */
    @Query("""
            SELECT l FROM Loan l
            WHERE l.status = com.example.library.loan.LoanStatus.WAITING_RETURN
            AND l.dueDate < :today
            """)
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);
    
    /**
     * Todos os empréstimos com itens e livros carregados — uso exclusivo de ADMIN.
     */
    @Query("""
            SELECT DISTINCT l FROM Loan l
            JOIN FETCH l.items i
            JOIN FETCH i.book
            JOIN FETCH l.user
            """)
    List<Loan> findAllWithItems();

    /**
     * Empréstimos ativos de um usuário (para verificar limite de empréstimos).
     * Não precisa de itens — apenas conta.
     */
    @Query("""
            SELECT COUNT(l) FROM Loan l
            WHERE l.user.id = :userId
            AND l.status = com.example.library.loan.LoanStatus.WAITING_RETURN
            """)
    long countActiveByUserId(@Param("userId") Long userId);
}