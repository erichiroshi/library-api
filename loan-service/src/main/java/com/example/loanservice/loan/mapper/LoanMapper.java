package com.example.loanservice.loan.mapper;

import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.loanservice.loan.Loan;
import com.example.loanservice.loan.LoanItem;
import com.example.loanservice.loan.dto.BookLoanDTO;
import com.example.loanservice.loan.dto.LoanResponseDTO;

@Mapper(componentModel = "spring")
public interface LoanMapper {

	@Mapping(target = "userId", source = "userId")
	@Mapping(target = "books", source = "items")
	LoanResponseDTO toDTO(Loan entity);

	Set<BookLoanDTO> bookToBookLoanDTO(Set<LoanItem> items);
	
	@Mapping(target="title", ignore = true)
	BookLoanDTO loanItemToBookLoanDTO(LoanItem item);

}