package com.example.library.api.controllers;

import com.example.library.domain.entities.Loan;
import com.example.library.domain.services.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

	private final LoanService service;

	public LoanController(LoanService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Loan create(@RequestParam Long userId, @RequestParam Set<Long> bookIds) {
		return service.create(userId, bookIds);
	}
}
