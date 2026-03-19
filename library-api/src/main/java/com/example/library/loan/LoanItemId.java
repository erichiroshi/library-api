package com.example.library.loan;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class LoanItemId implements Serializable {

	private Long loanId;
	private Long bookId;
}