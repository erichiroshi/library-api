package com.example.library.loan.mapper;

import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.library.loan.Loan;
import com.example.library.loan.LoanItem;
import com.example.library.loan.dto.BookLoanDTO;
import com.example.library.loan.dto.LoanResponseDTO;

@Mapper(componentModel = "spring")
public interface LoanMapper {

	@Mapping(target = "userId", source = "user.name")
	@Mapping(target = "books", source = "items")
	LoanResponseDTO toDTO(Loan entity);

	Set<BookLoanDTO> bookToBookLoanDTO(Set<LoanItem> items);
	
	@Mapping(target = "title", source = "item.book.title")
	BookLoanDTO loanItemToBookLoanDTO(LoanItem item);

}