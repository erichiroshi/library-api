package com.example.loanservice.loan;

import com.example.loanservice.common.entity.BaseEntity;

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
@Table(name = "tb_loan_item", schema = "lending")
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

	@Column(nullable = false)
	private Integer quantity;
}