package com.library.service;

import com.library.dto.LoanResponse;
import com.library.entity.Book;
import com.library.entity.Borrower;
import com.library.entity.LoanRecord;
import com.library.entity.LoanStatus;
import com.library.exception.BookAlreadyBorrowedException;
import com.library.exception.InvalidBookReturnException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.BookRepository;
import com.library.repository.BorrowerRepository;
import com.library.repository.LoanRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final BorrowerRepository borrowerRepository;
    private final BookRepository bookRepository;
    private final LoanRecordRepository loanRecordRepository;

    public LoanService(
            BorrowerRepository borrowerRepository,
            BookRepository bookRepository,
            LoanRecordRepository loanRecordRepository) {
        this.borrowerRepository = borrowerRepository;
        this.bookRepository = bookRepository;
        this.loanRecordRepository = loanRecordRepository;
    }

    @Transactional
    public LoanResponse borrowBook(UUID borrowerId, UUID bookId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Borrower not found with id: %s", borrowerId)
                ));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Book not found with id: %s", bookId)
                ));

        if (book.isBorrowed()) {
            throw new BookAlreadyBorrowedException(
                    String.format("Book with id '%s' is already borrowed and unavailable.", bookId)
            );
        }

        try {
            book.markAsBorrowedBy(borrower);
            bookRepository.saveAndFlush(book);

            LoanRecord loanRecord = new LoanRecord(book, borrower);
            LoanRecord savedLoan = loanRecordRepository.save(loanRecord);
            log.info("Borrower id={} borrowed book id={} (loanId={})", borrowerId, bookId, savedLoan.getId());
            return LoanResponse.fromEntity(savedLoan);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new BookAlreadyBorrowedException(
                    String.format("Book with id '%s' was concurrently borrowed by another member.", bookId),
                    ex
            );
        }
    }

    @Transactional
    public LoanResponse returnBook(UUID borrowerId, UUID bookId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Borrower not found with id: %s", borrowerId)
                ));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Book not found with id: %s", bookId)
                ));

        if (!book.isBorrowed()) {
            throw new InvalidBookReturnException(
                    String.format("Book with id '%s' is not currently borrowed.", bookId)
            );
        }

        if (book.getCurrentBorrower() != null && !book.getCurrentBorrower().getId().equals(borrower.getId())) {
            throw new InvalidBookReturnException(
                    String.format("Book with id '%s' is currently borrowed by a different borrower and cannot be returned by borrower '%s'.",
                            bookId, borrowerId)
            );
        }

        LoanRecord activeLoan = loanRecordRepository
                .findFirstByBookIdAndStatusOrderByBorrowedAtDesc(bookId, LoanStatus.ACTIVE)
                .orElseGet(() -> new LoanRecord(book, borrower));

        activeLoan.markReturned();
        LoanRecord savedLoan = loanRecordRepository.save(activeLoan);

        book.markAsReturned();
        bookRepository.saveAndFlush(book);

        log.info("Borrower id={} returned book id={} (loanId={})", borrowerId, bookId, savedLoan.getId());
        return LoanResponse.fromEntity(savedLoan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getBorrowerLoanHistory(UUID borrowerId) {
        if (!borrowerRepository.existsById(borrowerId)) {
            throw new ResourceNotFoundException(
                    String.format("Borrower not found with id: %s", borrowerId)
            );
        }
        return loanRecordRepository.findByBorrowerIdOrderByBorrowedAtDesc(borrowerId)
                .stream()
                .map(LoanResponse::fromEntity)
                .toList();
    }
}

