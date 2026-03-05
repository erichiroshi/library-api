package com.example.library.loan;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.book.Book;
import com.example.library.book.BookLookupService;
import com.example.library.book.exception.BookNotFoundException;
import com.example.library.loan.dto.LoanCreateDTO;
import com.example.library.loan.dto.LoanResponseDTO;
import com.example.library.loan.event.LoanCanceledEvent;
import com.example.library.loan.event.LoanCreatedEvent;
import com.example.library.loan.event.LoanReturnedEvent;
import com.example.library.loan.exception.BookNotAvailableException;
import com.example.library.loan.exception.LoanAlreadyReturnedException;
import com.example.library.loan.exception.LoanNotFoundException;
import com.example.library.loan.exception.LoanUnauthorizedException;
import com.example.library.loan.mapper.LoanMapper;
import com.example.library.user.User;
import com.example.library.user.UserLookupService;
import com.example.library.user.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);
	
	private final LoanRepository loanRepository;
    private final BookLookupService bookAvailabilityPort;    // usado apenas para verificação e decrement atômico na criação
	private final UserLookupService userLookupService;          // usado para validar existência de usuários em consultas administrativas
	private final LoanMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    // ─────────────────────────────────────────────
    // CRIAR EMPRÉSTIMO
    // ─────────────────────────────────────────────
	
	@Transactional
	public LoanResponseDTO create(LoanCreateDTO dto) {
		
		User user = getAuthenticatedUser();
        
        // Valida existência de todos os livros antes de iniciar
		for (Long bookId : dto.booksId()) {
			bookAvailabilityPort.findById(bookId)
				.orElseThrow(() -> new BookNotFoundException(bookId));
		}

        log.info("Creating loan for user={} books={}", user.getEmail(), dto.booksId());
        
		Loan loan = new Loan();
		loan.setUser(user);
		loan.setLoanDate(LocalDate.now());
		loan.setDueDate(LocalDate.now().plusDays(7));
		loan.setStatus(LoanStatus.WAITING_RETURN);

		for (Long bookId : dto.booksId()) {
			Book book = bookAvailabilityPort.findById(bookId)
					.orElseThrow(() -> new BookNotFoundException(bookId));

            // Update atômico — evita race condition em empréstimos concorrentes
			int updated = bookAvailabilityPort.decrementCopies(bookId);
			if (updated == 0) {
				throw new BookNotAvailableException(bookId, book.getTitle());
			}

			LoanItem item = new LoanItem();
			item.getId().setBookId(book.getId());
			item.setLoan(loan);
			item.setBook(book);
			item.setQuantity(1);

			loan.getItems().add(item);
            log.debug("Book added to loan: bookId={} title={}", bookId, book.getTitle());
		}

        Loan saved = loanRepository.save(loan);
        
        // Publica evento — outros domínios podem reagir sem acoplamento direto
        eventPublisher.publishEvent(new LoanCreatedEvent(
                saved.getId(),
                user.getId(),
                dto.booksId()
        ));

        log.info("Loan created: loanId={} user={} books={}",
                saved.getId(), user.getEmail(), dto.booksId().size());
        
        // Recarrega com JOIN FETCH para garantir que o mapper acessa itens dentro da transação
        return mapper.toDTO(findWithItemsOrThrow(saved.getId()));
	}

	// ─────────────────────────────────────────────
    // DEVOLVER EMPRÉSTIMO
    // ─────────────────────────────────────────────
	
	@Transactional
	public LoanResponseDTO returnLoan(Long loanId) {

        User user = getAuthenticatedUser();
		Loan loan = findWithItemsOrThrow(loanId);
		
        validateOwnershipOrAdmin(loan, user);

		if (loan.getStatus() == LoanStatus.RETURNED) {
			throw new LoanAlreadyReturnedException(loanId);
		}
        
		if (loan.getStatus() == LoanStatus.CANCELED) {
            throw new LoanAlreadyReturnedException(loanId);
        }

		loan.setReturnDate(LocalDate.now());
		loan.setStatus(LoanStatus.RETURNED);

	    // Monta o mapa bookId → quantidade antes de publicar o evento
        Map<Long, Integer> bookQuantities = buildBookQuantities(loan);

        // Publica evento — BookEventListener restaura as cópias
        eventPublisher.publishEvent(new LoanReturnedEvent(
                loan.getId(),
                user.getId(),
                bookQuantities
        ));

        log.info("Loan returned: loanId={} user={}", loanId, user.getEmail());

		return mapper.toDTO(loan);
	}
	
    // ─────────────────────────────────────────────
    // CANCELAR EMPRÉSTIMO
    // ─────────────────────────────────────────────
	
    @Transactional
    public LoanResponseDTO cancelLoan(Long loanId) {

        User user = getAuthenticatedUser();
        Loan loan = findWithItemsOrThrow(loanId);

        validateOwnershipOrAdmin(loan, user);

        if (loan.getStatus() != LoanStatus.WAITING_RETURN) {
            throw new LoanAlreadyReturnedException(loanId);
        }

        loan.setStatus(LoanStatus.CANCELED);

        Map<Long, Integer> bookQuantities = buildBookQuantities(loan);

        // Publica evento — BookEventListener restaura as cópias
        eventPublisher.publishEvent(new LoanCanceledEvent(
                loan.getId(),
                user.getId(),
                bookQuantities
        ));

        log.info("Loan canceled: loanId={} user={}", loanId, user.getEmail());

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
    	User user = getAuthenticatedUser();
        Loan loan = findWithItemsOrThrow(loanId);
        validateOwnershipOrAdmin(loan, user);
		return mapper.toDTO(loan);
	}
    
    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findMyLoans() {
        User user = getAuthenticatedUser();
        log.debug("Fetching loans for user={}", user.getEmail());
        return loanRepository.findByUserIdWithItems(user.getId())
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findByUser(Long userId) {
    	userLookupService.findById(userId)
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
     * Monta mapa bookId → quantidade a partir dos itens do empréstimo.
     * Usado para popular os eventos de devolução e cancelamento.
     */
    private Map<Long, Integer> buildBookQuantities(Loan loan) {
        return loan.getItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getBook().getId(),
                        LoanItem::getQuantity
                ));
    }
    
    /**
     * Busca um empréstimo pelo ID com itens e usuário já carregados via JOIN FETCH.
     * Garante que o mapper acessa as coleções LAZY dentro da transação ativa.
     */
    private Loan findWithItemsOrThrow(Long loanId) {
        return loanRepository.findByIdWithItemsAndUser(loanId)
                .orElseThrow(() -> new LoanNotFoundException(loanId));
    }
	
	/**
     * Recupera o usuário autenticado direto do SecurityContext.
     * O principal é o email (populado pelo JwtAuthenticationFilter),
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) auth.getPrincipal();
        return userLookupService.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
    
    /**
     * Garante que apenas o dono do empréstimo ou um ADMIN pode operá-lo.
     * Retorna 404 intencionalmente para não vazar que o empréstimo existe.
     */
    private void validateOwnershipOrAdmin(Loan loan, User user) {
        boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");
        boolean isOwner = loan.getUser().getId().equals(user.getId());

        if (!isOwner && !isAdmin) {
            log.warn("Unauthorized loan access attempt: loanId={} userId={}", loan.getId(), user.getId());
            throw new LoanUnauthorizedException(loan.getId());
        }
    }
}
