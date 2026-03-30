package com.example.library.loan.dto;

import java.time.LocalDate;
import java.util.Set;

import com.example.library.loan.LoanStatus;

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
