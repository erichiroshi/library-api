package br.com.erichiroshi.libraryapi1.service;

import java.util.Optional;

import br.com.erichiroshi.libraryapi1.model.entity.Loan;

public interface LoanService {

	Loan save(Loan loan);

	Optional<Loan> getById(long anyLong);

	Loan update(Loan loan);
}
