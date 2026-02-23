package com.example.library.loan;

import com.example.library.book.Book;
import com.example.library.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_loan_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
public class LoanItem extends BaseEntity {

	@EmbeddedId
	private LoanItemId id = new LoanItemId();

	@ManyToOne
	@MapsId("loanId")
	@JoinColumn(name = "loan_id")
	private Loan loan;

	@ManyToOne
	@MapsId("bookId")
	@JoinColumn(name = "book_id")
	private Book book;

	@Column(nullable = false)
	private Integer quantity;
}