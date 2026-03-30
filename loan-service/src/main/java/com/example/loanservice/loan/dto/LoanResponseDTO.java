package com.example.loanservice.loan.dto;

import java.time.LocalDate;
import java.util.Set;

import com.example.loanservice.loan.LoanStatus;

public record LoanResponseDTO(

		Long id,

		LocalDate loanDate,

		LocalDate dueDate,

		LocalDate returnDate,

		LoanStatus status,

		String userId,

		Set<BookLoanDTO> books

) {
}
