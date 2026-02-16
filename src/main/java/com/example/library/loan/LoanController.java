package com.example.library.loan;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.library.loan.dto.LoanCreateDTO;
import com.example.library.loan.dto.LoanResponseDTO;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

	private final LoanService service;

	public LoanController(LoanService service) {
		this.service = service;
	}

	/**
	 * POST /api/loans
	 * Cria um novo empréstimo para o usuário autenticado.
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ResponseEntity<LoanResponseDTO> create(@Valid @RequestBody LoanCreateDTO dto) {
		LoanResponseDTO response = service.create(dto);
		URI uri = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(response.id())
				.toUri();
		return ResponseEntity.created(uri).body(response);
	}

	/**
	 * GET /api/loans/{id}
	 * Busca um empréstimo por id.
	 * Usuário comum vê só o próprio; ADMIN vê qualquer um (validação no Service).
	 */
	 @GetMapping("/{loanId}")
	public ResponseEntity<LoanResponseDTO> findById(@PathVariable Long loanId) {
		return ResponseEntity.ok(service.findById(loanId));
	}

     /**
     * GET /api/loans/me
     * Lista todos os empréstimos do usuário autenticado.
     */
    @GetMapping("/me")
    public ResponseEntity<List<LoanResponseDTO>> findMyLoans() {
        return ResponseEntity.ok(service.findMyLoans());
    }
    
    /**
     * GET /api/loans
     * Lista todos os empréstimos — apenas ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanResponseDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }
    
    /**
     * GET /api/loans/user/{userId}
     * Lista empréstimos de um usuário específico — apenas ADMIN.
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanResponseDTO>> findByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(service.findByUser(userId));
    }
    
    /**
     * GET /api/loans/overdue
     * Lista empréstimos vencidos — apenas ADMIN.
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanResponseDTO>> findOverdue() {
        return ResponseEntity.ok(service.findOverdue());
    }
    
    /**
     * PATCH /api/loans/{loanId}/return
     * Devolve um empréstimo ativo.
     * Usuário comum só pode devolver o próprio; ADMIN pode devolver qualquer um.
     */
    @PatchMapping("/{loanId}/return")
    public ResponseEntity<LoanResponseDTO> returnLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(service.returnLoan(loanId));
    }
    
    /**
     * PATCH /api/loans/{loanId}/cancel
     * Cancela um empréstimo WAITING_RETURN.
     * Usuário comum só pode cancelar o próprio; ADMIN pode cancelar qualquer um.
     */
    @PatchMapping("/{loanId}/cancel")
    public ResponseEntity<LoanResponseDTO> cancelLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(service.cancelLoan(loanId));
    }
}
