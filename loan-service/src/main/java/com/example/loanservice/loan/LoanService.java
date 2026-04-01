package com.example.loanservice.loan;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.loanservice.client.BookClient;
import com.example.loanservice.client.UserClient;
import com.example.loanservice.client.dto.BookDTO;
import com.example.loanservice.client.dto.UserDTO;
import com.example.loanservice.loan.dto.LoanCreateDTO;
import com.example.loanservice.loan.dto.LoanResponseDTO;
import com.example.loanservice.loan.exception.BookNotAvailableException;
import com.example.loanservice.loan.exception.BookNotFoundException;
import com.example.loanservice.loan.exception.LoanAlreadyReturnedException;
import com.example.loanservice.loan.exception.LoanNotFoundException;
import com.example.loanservice.loan.exception.LoanUnauthorizedException;
import com.example.loanservice.loan.exception.UserNotFoundException;
import com.example.loanservice.loan.mapper.LoanMapper;
import com.example.loanservice.messaging.LoanEventPublisher;
import com.example.loanservice.messaging.event.BookRestoreEvent;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);
	
	private final LoanRepository loanRepository;
    private final BookClient bookClient;
	private final UserClient userClient;
	private final LoanMapper mapper;
    private final LoanEventPublisher eventPublisher;

    // ─────────────────────────────────────────────
    // CRIAR EMPRÉSTIMO
    // ─────────────────────────────────────────────
	
    @Transactional
    public LoanResponseDTO create(LoanCreateDTO dto) {
    	
        String userEmail = getUserEmail();
        UserDTO userDTO = getAuthenticatedUser(userEmail);
        
        log.debug("user= {}", userDTO);

        // Carrega todos os livros de uma vez — elimina double fetch
        Map<Long, BookDTO> books = dto.booksId().stream()
		    .distinct()
		    .collect(Collectors.toMap(
		        id -> id,
		        id -> bookClient.findInternalBooksById(id).orElseThrow(() ->new BookNotFoundException(id))
		    ));

        log.info("Creating loan for user={} books={}", userDTO.email(), dto.booksId());
        
        log.debug("books={}", books.values());

        Loan loan = new Loan();
        loan.setUserId(userDTO.id());
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(7));
        loan.setStatus(LoanStatus.WAITING_RETURN);

        for (Long bookId : dto.booksId()) {
            BookDTO bookDTO = books.get(bookId);

            // Update atômico — evita race condition em empréstimos concorrentes
            int updated = bookClient.decrementInternalCopies(bookId);
            if (updated == 0) {
                throw new BookNotAvailableException(bookId, bookDTO.title());
            }

            LoanItem item = new LoanItem();
            item.getId().setBookId(bookDTO.id());
            item.setLoan(loan);
            item.getId().setBookId(bookDTO.id());
            item.setQuantity(1);

            loan.getItems().add(item);
            log.debug("Book added to loan: bookId={} title={}", bookId, bookDTO.title());
        }

        Loan saved = loanRepository.save(loan);

        log.info("Loan created: loanId={} user={} books={}",
                saved.getId(), userDTO.email(), dto.booksId().size());

        // Recarrega com JOIN FETCH para garantir que o mapper acessa itens dentro da transação
        return mapper.toDTO(findWithItemsOrThrow(saved.getId()));
    }

	private String getUserEmail() {
		return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
	}

	// ─────────────────────────────────────────────
    // DEVOLVER EMPRÉSTIMO
    // ─────────────────────────────────────────────
	
	@Transactional
	public LoanResponseDTO returnLoan(Long loanId) {

        UserDTO userDTO = getAuthenticatedUser(getUserEmail());
		Loan loan = findWithItemsOrThrow(loanId);
		
        validateOwnershipOrAdmin(loan, userDTO);

		if (loan.getStatus() == LoanStatus.RETURNED) {
			throw new LoanAlreadyReturnedException(loanId);
		}
        
		if (loan.getStatus() == LoanStatus.CANCELED) {
            throw new LoanAlreadyReturnedException(loanId);
        }

		loan.setReturnDate(LocalDate.now());
		loan.setStatus(LoanStatus.RETURNED);
		
        // Publica evento assíncrono — catalog-service restaura as cópias
        Map<Long, Integer> bookQuantities = buildBookQuantities(loan);
        
        eventPublisher.publishBookRestore(
                new BookRestoreEvent(loan.getId(), bookQuantities)
        );
//		loan.getItems().forEach(item -> bookClient.restoreCopies(item.getId().getBookId(), item.getQuantity()));

        log.info("Loan returned: loanId={} user={}", loanId, userDTO.email());
		return mapper.toDTO(loan);
	}
	
    // ─────────────────────────────────────────────
    // CANCELAR EMPRÉSTIMO
    // ─────────────────────────────────────────────
	
    @Transactional
    public LoanResponseDTO cancelLoan(Long loanId) {

        UserDTO userDTO = getAuthenticatedUser(getUserEmail());
        Loan loan = findWithItemsOrThrow(loanId);

        validateOwnershipOrAdmin(loan, userDTO);

        if (loan.getStatus() != LoanStatus.WAITING_RETURN) {
            throw new LoanAlreadyReturnedException(loanId);
        }

        loan.setStatus(LoanStatus.CANCELED);
        
        // Publica evento assíncrono — catalog-service restaura as cópias
        Map<Long, Integer> bookQuantities = buildBookQuantities(loan);
        eventPublisher.publishBookRestore(
                new BookRestoreEvent(loan.getId(), bookQuantities)
        );        
        
//		loan.getItems().forEach(item -> bookClient.restoreCopies(item.getId().getBookId(), item.getQuantity()));

        log.info("Loan canceled: loanId={} user={}", loanId, userDTO.email());

        return mapper.toDTO(loan);
    }

    // ─────────────────────────────────────────────
    // MARCAR COMO VENCIDO (uso interno / scheduler)
    // ─────────────────────────────────────────────
    
    @Transactional
    public void markOverdue() {
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(LocalDate.now());
        overdueLoans.forEach(loan -> {
            loan.setStatus(LoanStatus.OVERDUE);
            log.warn("Loan marked as OVERDUE: loanId={} dueDate={}", loan.getId(), loan.getDueDate());
        });
        log.info("Marked {} loans as OVERDUE", overdueLoans.size());
    }
    
    // ─────────────────────────────────────────────
    // CONSULTAS
    // ─────────────────────────────────────────────
    
    @Transactional(readOnly = true)
	public LoanResponseDTO findById(Long loanId) {
    	UserDTO userDTO = getAuthenticatedUser(getUserEmail());
        Loan loan = findWithItemsOrThrow(loanId);
        validateOwnershipOrAdmin(loan, userDTO);
		return mapper.toDTO(loan);
	}
    
    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findMyLoans() {
    	UserDTO userDTO = getAuthenticatedUser(getUserEmail());
        log.debug("Fetching loans for user={}", userDTO.email());
        return loanRepository.findByUserIdWithItems(userDTO.id())
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findByUser(Long userId) {
    	userClient.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return loanRepository.findByUserIdWithItems(userId)
                .stream()
                .map(mapper::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findOverdue() {
        return loanRepository.findOverdueLoans(LocalDate.now())
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findAll() {
        return loanRepository.findAllWithItems()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
    
    // ─────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────
    
    /**
     * Busca um empréstimo pelo ID com itens e usuário já carregados via JOIN FETCH.
     * Garante que o mapper acessa as coleções LAZY dentro da transação ativa.
     */
    private Loan findWithItemsOrThrow(Long loanId) {
        return loanRepository.findByIdWithItems(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
    }
	
	/**
     * Recupera o usuário autenticado direto do SecurityContext.
     * O principal é o email (populado pelo JwtAuthenticationFilter),
     */
    private UserDTO getAuthenticatedUser(String userEmail) {
        return userClient.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));
    }
    
    /**
     * Garante que apenas o dono do empréstimo ou um ADMIN pode operá-lo.
     * Retorna 404 intencionalmente para não vazar que o empréstimo existe.
     */
    private void validateOwnershipOrAdmin(Loan loan, UserDTO userDTO) {
        boolean isAdmin = userDTO.roles().contains("ROLE_ADMIN");
        boolean isOwner = loan.getUserId().equals(userDTO.id());

        if (!isOwner && !isAdmin) {
            log.warn("Unauthorized loan access attempt: loanId={} userId={}", loan.getId(), userDTO.id());
            throw new LoanUnauthorizedException(loan.getId());
        }
    }

	// ─────────────────────────────────────────────
	// CANCELAR EMPRÉSTIMO
	// ─────────────────────────────────────────────
	
	private Map<Long, Integer> buildBookQuantities(Loan loan) {
		Map<Long, Integer> bookQuantities = new HashMap<>();
		loan.getItems().forEach(item -> bookQuantities.put(item.getId().getBookId(), item.getQuantity()));
		return bookQuantities;
	}
}
