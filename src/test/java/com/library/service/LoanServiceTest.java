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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanRecordRepository loanRecordRepository;

    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = new LoanService(borrowerRepository, bookRepository, loanRecordRepository);
    }

    @Test
    @DisplayName("Should borrow an available book copy successfully")
    void shouldBorrowAvailableBookSuccessfully() {
        UUID borrowerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Borrower borrower = new Borrower(borrowerId, "Alice Tan", "alice@example.com");
        Book book = new Book(bookId, "978-0-13-235088-4", "Clean Code", "Robert C. Martin");

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.saveAndFlush(book)).thenReturn(book);
        when(loanRecordRepository.save(any(LoanRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse response = loanService.borrowBook(borrowerId, bookId);

        assertThat(book.isBorrowed()).isTrue();
        assertThat(book.getCurrentBorrower()).isEqualTo(borrower);
        assertThat(response.bookId()).isEqualTo(bookId);
        assertThat(response.borrowerId()).isEqualTo(borrowerId);
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.returnedAt()).isNull();
    }

    @Test
    @DisplayName("Should throw BookAlreadyBorrowedException when book is already borrowed")
    void shouldThrowExceptionWhenBookAlreadyBorrowed() {
        UUID borrower1Id = UUID.randomUUID();
        UUID borrower2Id = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        Borrower borrower1 = new Borrower(borrower1Id, "Alice Tan", "alice@example.com");
        Borrower borrower2 = new Borrower(borrower2Id, "Bob Lim", "bob@example.com");
        Book book = new Book(bookId, "978-0-13-235088-4", "Clean Code", "Robert C. Martin");
        book.markAsBorrowedBy(borrower1);

        when(borrowerRepository.findById(borrower2Id)).thenReturn(Optional.of(borrower2));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> loanService.borrowBook(borrower2Id, bookId))
                .isInstanceOf(BookAlreadyBorrowedException.class)
                .hasMessageContaining(bookId.toString());

        verify(loanRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should translate ObjectOptimisticLockingFailureException into BookAlreadyBorrowedException")
    void shouldHandleOptimisticLockingFailureOnConcurrentBorrow() {
        UUID borrowerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Borrower borrower = new Borrower(borrowerId, "Alice Tan", "alice@example.com");
        Book book = new Book(bookId, "978-0-13-235088-4", "Clean Code", "Robert C. Martin");

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.saveAndFlush(book))
                .thenThrow(new ObjectOptimisticLockingFailureException(Book.class, bookId));

        assertThatThrownBy(() -> loanService.borrowBook(borrowerId, bookId))
                .isInstanceOf(BookAlreadyBorrowedException.class)
                .hasMessageContaining("concurrently borrowed");
    }

    @Test
    @DisplayName("Should return a borrowed book successfully")
    void shouldReturnBorrowedBookSuccessfully() {
        UUID borrowerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Borrower borrower = new Borrower(borrowerId, "Alice Tan", "alice@example.com");
        Book book = new Book(bookId, "978-0-13-235088-4", "Clean Code", "Robert C. Martin");
        book.markAsBorrowedBy(borrower);

        LoanRecord activeLoan = new LoanRecord(book, borrower);

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRecordRepository.findFirstByBookIdAndStatusOrderByBorrowedAtDesc(bookId, LoanStatus.ACTIVE))
                .thenReturn(Optional.of(activeLoan));
        when(loanRecordRepository.save(activeLoan)).thenReturn(activeLoan);
        when(bookRepository.saveAndFlush(book)).thenReturn(book);

        LoanResponse response = loanService.returnBook(borrowerId, bookId);

        assertThat(book.isBorrowed()).isFalse();
        assertThat(book.getCurrentBorrower()).isNull();
        assertThat(response.status()).isEqualTo(LoanStatus.RETURNED);
        assertThat(response.returnedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should throw InvalidBookReturnException when returning a book that is not borrowed")
    void shouldThrowExceptionWhenReturningUnborrowedBook() {
        UUID borrowerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        Borrower borrower = new Borrower(borrowerId, "Alice Tan", "alice@example.com");
        Book book = new Book(bookId, "978-0-13-235088-4", "Clean Code", "Robert C. Martin");

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(borrower));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> loanService.returnBook(borrowerId, bookId))
                .isInstanceOf(InvalidBookReturnException.class)
                .hasMessageContaining("not currently borrowed");
    }

    @Test
    @DisplayName("Should throw InvalidBookReturnException when a different borrower attempts to return the book")
    void shouldThrowExceptionWhenDifferentBorrowerAttemptsReturn() {
        UUID borrower1Id = UUID.randomUUID();
        UUID borrower2Id = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        Borrower borrower1 = new Borrower(borrower1Id, "Alice Tan", "alice@example.com");
        Borrower borrower2 = new Borrower(borrower2Id, "Bob Lim", "bob@example.com");
        Book book = new Book(bookId, "978-0-13-235088-4", "Clean Code", "Robert C. Martin");
        book.markAsBorrowedBy(borrower1);

        when(borrowerRepository.findById(borrower2Id)).thenReturn(Optional.of(borrower2));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> loanService.returnBook(borrower2Id, bookId))
                .isInstanceOf(InvalidBookReturnException.class)
                .hasMessageContaining("different borrower");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when borrower or book does not exist")
    void shouldThrowResourceNotFoundWhenBorrowerOrBookMissing() {
        UUID borrowerId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrowBook(borrowerId, bookId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(borrowerId.toString());
    }
}

