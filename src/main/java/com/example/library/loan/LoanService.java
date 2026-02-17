package com.example.library.loan;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.book.exception.BookNotFoundException;
import com.example.library.loan.dto.LoanCreateDTO;
import com.example.library.loan.dto.LoanResponseDTO;
import com.example.library.loan.execption.BookNotAvailableException;
import com.example.library.loan.execption.LoanAlreadyReturnedException;
import com.example.library.loan.execption.LoanNotFoundException;
import com.example.library.loan.execption.LoanUnauthorizedException;
import com.example.library.loan.mapper.LoanMapper;
import com.example.library.user.User;
import com.example.library.user.UserRepository;
import com.example.library.user.exception.UserNotFoundException;

@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);
	
	private final LoanRepository loanRepository;
	private final BookRepository bookRepository;
	private final UserRepository userRepository;
	private final LoanMapper mapper;

	public LoanService(LoanRepository loanRepository, BookRepository bookRepository, UserRepository userRepository, LoanMapper mapper) {
		this.loanRepository = loanRepository;
		this.bookRepository = bookRepository;
		this.userRepository = userRepository;
		this.mapper = mapper;
	}

    // ─────────────────────────────────────────────
    // CRIAR EMPRÉSTIMO
    // ─────────────────────────────────────────────
	
	@Transactional
	public LoanResponseDTO create(LoanCreateDTO dto) {
		
		User user = getAuthenticatedUser();
        
		for (Long bookId : dto.booksId()) {
			bookRepository.findById(bookId).orElseThrow(() -> new BookNotFoundException(bookId));
		}

        log.info("Creating loan for user={} books={}", user.getEmail(), dto.booksId());
        
		Loan loan = new Loan();
		loan.setUser(user);
		loan.setLoanDate(LocalDate.now());
		loan.setDueDate(LocalDate.now().plusDays(7));
		loan.setStatus(LoanStatus.WAITING_RETURN);

		for (Long bookId : dto.booksId()) {

			Book book = bookRepository.findById(bookId)
					.orElseThrow(() -> new BookNotFoundException(bookId));

            // Update atômico — evita race condition em empréstimos concorrentes
			int updated = bookRepository.decrementCopies(bookId);
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

        log.info("Loan created: loanId={} user={} books={}",
                saved.getId(), user.getEmail(), dto.booksId().size());
        
		return mapper.toDTO(loan);
	}

	// ─────────────────────────────────────────────
    // DEVOLVER EMPRÉSTIMO
    // ─────────────────────────────────────────────
	
	@Transactional
	public LoanResponseDTO returnLoan(Long loanId) {

        User user = getAuthenticatedUser();
		Loan loan = find(loanId);
		
        validateOwnershipOrAdmin(loan, user);

		if (loan.getStatus() == LoanStatus.RETURNED) {
			throw new LoanAlreadyReturnedException(loanId);
		}
        
		if (loan.getStatus() == LoanStatus.CANCELED) {
            throw new LoanAlreadyReturnedException(loanId);
        }

		loan.setReturnDate(LocalDate.now());
		loan.setStatus(LoanStatus.RETURNED);

        // Devolve as cópias — dirty checking cuida do save dentro do @Transactional
		loan.getItems().forEach(item -> {
			Book book = item.getBook();
			book.setAvailableCopies(book.getAvailableCopies() + item.getQuantity());
            log.debug("Copies restored: bookId={} title={}", book.getId(), book.getTitle());
		});

        log.info("Loan returned: loanId={} user={}", loanId, user.getEmail());

		return mapper.toDTO(loan);
	}
	
    // ─────────────────────────────────────────────
    // CANCELAR EMPRÉSTIMO
    // ─────────────────────────────────────────────
	
    @Transactional
    public LoanResponseDTO cancelLoan(Long loanId) {

        User user = getAuthenticatedUser();
        Loan loan = find(loanId);

        validateOwnershipOrAdmin(loan, user);

        if (loan.getStatus() != LoanStatus.WAITING_RETURN) {
            throw new LoanAlreadyReturnedException(loanId);
        }

        loan.setStatus(LoanStatus.CANCELED);

        // Devolve as cópias ao cancelar
        loan.getItems().forEach(item -> {
            Book book = item.getBook();
            book.setAvailableCopies(book.getAvailableCopies() + item.getQuantity());
        });

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
        Loan loan = find(loanId);

        validateOwnershipOrAdmin(loan, user);
		return mapper.toDTO(loan);
	}
    
    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findMyLoans() {
    	
        User user = getAuthenticatedUser();
        
        log.debug("Fetching loans for user={}", user.getEmail());
        
        return loanRepository.findByUserId(user.getId())
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findByUser(Long userId) {
    	
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        
        return loanRepository.findByUserId(userId)
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
        return loanRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .toList();
    }
    
    // ─────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────
    
	private Loan find(Long loanId) {
		return loanRepository.findById(loanId)
				.orElseThrow(() -> new LoanNotFoundException(loanId));
	}
	
	/**
     * Recupera o usuário autenticado direto do SecurityContext.
     * O principal já é um User (populado pelo JwtAuthenticationFilter),
     * então não precisa de uma query adicional ao banco.
     */
    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
    
    /**
     * Garante que apenas o dono do empréstimo ou um ADMIN pode operá-lo.
     */
    private void validateOwnershipOrAdmin(Loan loan, User user) {
        boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");
        boolean isOwner = loan.getUser().getId().equals(user.getId());

        if (!isOwner && !isAdmin) {
            log.warn("Unauthorized loan access attempt: loanId={} userId={}", loan.getId(), user.getId());
            throw new LoanUnauthorizedException(loan.getId()); // 404 intencional — não vazar que o loan existe
        }
    }
}
