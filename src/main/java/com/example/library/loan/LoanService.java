package com.example.library.loan;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.library.book.Book;
import com.example.library.book.BookRepository;
import com.example.library.exceptions.exceptions.BusinessException;
import com.example.library.user.User;
import com.example.library.user.UserRepository;

@Service
public class LoanService {

	private final LoanRepository loanRepository;
	private final UserRepository userRepository;
	private final BookRepository bookRepository;

	public LoanService(LoanRepository loanRepository, UserRepository userRepository, BookRepository bookRepository) {
		this.loanRepository = loanRepository;
		this.userRepository = userRepository;
		this.bookRepository = bookRepository;
	}

	@Transactional
    public Loan create(Long userId, Set<Long> bookIds) {

        User user = userRepository.findById(userId).get();

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(7));
        loan.setStatus(LoanStatus.WAITING_RETURN);

        for (Long bookId : bookIds) {

			Book book = bookRepository.findById(bookId).get();

			if (book.getAvailableCopies() <= 0) {
				throw new BusinessException("Book is not available: " + book.getTitle());
			}


            book.setAvailableCopies(book.getAvailableCopies() - 1);

            LoanItem item = new LoanItem();
            item.setLoan(loan);
            item.setBook(book);
            item.setQuantity(1);

            loan.getItems().add(item);
        }

        return loanRepository.save(loan);
    }
}
