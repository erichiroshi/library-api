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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.library.loan.dto.LoanCreateDTO;
import com.example.library.loan.dto.LoanResponseDTO;

import jakarta.validation.Valid;

@Tag(name = "Loans", description = "Endpoints para gerenciamento de empréstimos de livros")
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

	private final LoanService service;

	public LoanController(LoanService service) {
		this.service = service;
	}

	@Operation(
			summary = "Criar um novo empréstimo", 
			description = "Permite que um usuário solicite um empréstimo de um livro. O status inicial é WAITING_RETURN."
			)
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

	@Operation(
			summary = "Buscar empréstimo por ID", 
			description = "Retorna os detalhes de um empréstimo específico. Usuário comum só pode acessar seus próprios empréstimos; ADMIN pode acessar qualquer um."
			)
	@GetMapping("/{loanId}")
	public ResponseEntity<LoanResponseDTO> findById(@PathVariable Long loanId) {
		return ResponseEntity.ok(service.findById(loanId));
	}

	@Operation(
			summary = "Listar meus empréstimos",
			description = "Retorna uma lista de todos os empréstimos do usuário autenticado. Usuário comum vê apenas os próprios; ADMIN vê todos."
			)
    @GetMapping("/me")
    public ResponseEntity<List<LoanResponseDTO>> findMyLoans() {
        return ResponseEntity.ok(service.findMyLoans());
    }

    @Operation(
    		summary = "Listar todos os empréstimos",
    		description = "Retorna uma lista de todos os empréstimos no sistema. Apenas ADMIN pode acessar este endpoint."
			)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanResponseDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }
    
	@Operation(
			summary = "Listar empréstimos por usuário",
			description = "Retorna uma lista de empréstimos para um usuário específico. Apenas ADMIN pode acessar este endpoint."
			)
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanResponseDTO>> findByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(service.findByUser(userId));
    }
    
	@Operation(
			summary = "Listar empréstimos atrasados",
			description = "Retorna uma lista de empréstimos que estão atrasados (status WAITING_RETURN e data de devolução prevista já passou). Apenas ADMIN pode acessar este endpoint."
			)
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LoanResponseDTO>> findOverdue() {
        return ResponseEntity.ok(service.findOverdue());
    }
    
	@Operation(
			summary = "Registrar devolução de empréstimo",
			description = "Marca um empréstimo como DEVOLVED. Usuário comum só pode devolver seus próprios empréstimos; ADMIN pode devolver qualquer um."
			)
    @PatchMapping("/{loanId}/return")
    public ResponseEntity<LoanResponseDTO> returnLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(service.returnLoan(loanId));
    }
    
    @Operation(
			summary = "Cancelar empréstimo",
			description = "Permite que um usuário cancele um empréstimo que ainda não foi devolvido. O status do empréstimo é atualizado para CANCELED. Usuário comum só pode cancelar seus próprios empréstimos; ADMIN pode cancelar qualquer um."
			)
    @PatchMapping("/{loanId}/cancel")
    public ResponseEntity<LoanResponseDTO> cancelLoan(@PathVariable Long loanId) {
        return ResponseEntity.ok(service.cancelLoan(loanId));
    }
}
