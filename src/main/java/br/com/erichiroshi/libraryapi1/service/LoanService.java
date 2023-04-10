package br.com.erichiroshi.libraryapi1.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import br.com.erichiroshi.libraryapi1.api.dto.LoanFilterDTO;
import br.com.erichiroshi.libraryapi1.model.entity.Loan;

public interface LoanService {

	Loan save(Loan loan);

	Optional<Loan> getById(long anyLong);

	Loan update(Loan loan);

	Page<Loan> find(LoanFilterDTO filterDTO, Pageable pageable);
}
