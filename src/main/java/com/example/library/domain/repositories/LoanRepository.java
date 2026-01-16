package com.example.library.domain.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.library.domain.entities.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long> {
}
