package br.com.erichiroshi.libraryapi1.service.impl;

import java.util.Optional;

import br.com.erichiroshi.libraryapi1.model.entity.Loan;
import br.com.erichiroshi.libraryapi1.model.repository.LoanRepository;
import br.com.erichiroshi.libraryapi1.service.LoanService;
import br.com.erichiroshi.libraryapi1.service.exception.BusinessException;

public class LoanServiceImpl implements LoanService {

	private LoanRepository repository;

	public LoanServiceImpl(LoanRepository repository) {
		this.repository = repository;
	}

	@Override
	public Loan save(Loan loan) {
		if (repository.existsByBookAndNotReturned(loan.getBook())) {
			throw new BusinessException("Book already loaned");
		}
		return repository.save(loan);
	}

	@Override
	public Optional<Loan> getById(long id) {
		return repository.findById(id);
	}

	@Override
	public Loan update(Loan loan) {
		return null;
	}

}
