package com.example.loanservice.loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(hidden = true)
public record BookLoanDTO(
		String title
) {}