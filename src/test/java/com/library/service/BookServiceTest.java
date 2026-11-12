package com.library.service;

import com.library.dto.BookRegistrationRequest;
import com.library.dto.BookResponse;
import com.library.entity.Book;
import com.library.exception.IsbnConflictException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository);
    }

    @Test
    @DisplayName("Should register a new book when ISBN does not exist yet")
    void shouldRegisterNewBookWhenIsbnNotFound() {
        BookRegistrationRequest request = new BookRegistrationRequest(
                "978-0-13-235088-4",
                "Clean Code",
                "Robert C. Martin"
        );
        when(bookRepository.findFirstByIsbnIgnoreCase("978-0-13-235088-4")).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse response = bookService.registerBook(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.isbn()).isEqualTo("978-0-13-235088-4");
        assertThat(response.title()).isEqualTo("Clean Code");
        assertThat(response.author()).isEqualTo("Robert C. Martin");
        assertThat(response.borrowed()).isFalse();
    }

    @Test
    @DisplayName("Should register multiple copies of the same ISBN with distinct IDs when title and author match")
    void shouldRegisterSecondCopyWithSameIsbnTitleAndAuthor() {
        Book existingCopy = new Book(UUID.randomUUID(), "978-0-13-235088-4", "Clean Code", "Robert C. Martin");
        when(bookRepository.findFirstByIsbnIgnoreCase("978-0-13-235088-4")).thenReturn(Optional.of(existingCopy));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookRegistrationRequest secondCopyRequest = new BookRegistrationRequest(
                "978-0-13-235088-4",
                "Clean Code",
                "Robert C. Martin"
        );

        BookResponse secondCopyResponse = bookService.registerBook(secondCopyRequest);

        assertThat(secondCopyResponse.id()).isNotNull();
        assertThat(secondCopyResponse.id()).isNotEqualTo(existingCopy.getId());
        assertThat(secondCopyResponse.isbn()).isEqualTo("978-0-13-235088-4");
        assertThat(secondCopyResponse.title()).isEqualTo("Clean Code");
        assertThat(secondCopyResponse.author()).isEqualTo("Robert C. Martin");
    }

    @Test
    @DisplayName("Should throw IsbnConflictException when registering same ISBN with a different title")
    void shouldThrowIsbnConflictExceptionWhenTitleDiffers() {
        Book existingCopy = new Book(UUID.randomUUID(), "978-0-13-235088-4", "Clean Code", "Robert C. Martin");
        when(bookRepository.findFirstByIsbnIgnoreCase("978-0-13-235088-4")).thenReturn(Optional.of(existingCopy));

        BookRegistrationRequest conflictingRequest = new BookRegistrationRequest(
                "978-0-13-235088-4",
                "The Clean Coder",
                "Robert C. Martin"
        );

        assertThatThrownBy(() -> bookService.registerBook(conflictingRequest))
                .isInstanceOf(IsbnConflictException.class)
                .hasMessageContaining("978-0-13-235088-4")
                .hasMessageContaining("Clean Code");

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IsbnConflictException when registering same ISBN with a different author")
    void shouldThrowIsbnConflictExceptionWhenAuthorDiffers() {
        Book existingCopy = new Book(UUID.randomUUID(), "978-0-13-235088-4", "Clean Code", "Robert C. Martin");
        when(bookRepository.findFirstByIsbnIgnoreCase("978-0-13-235088-4")).thenReturn(Optional.of(existingCopy));

        BookRegistrationRequest conflictingRequest = new BookRegistrationRequest(
                "978-0-13-235088-4",
                "Clean Code",
                "Martin Fowler"
        );

        assertThatThrownBy(() -> bookService.registerBook(conflictingRequest))
                .isInstanceOf(IsbnConflictException.class)
                .hasMessageContaining("978-0-13-235088-4")
                .hasMessageContaining("Robert C. Martin");

        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should allow two books with the same title and author if they have different ISBNs")
    void shouldAllowSameTitleAndAuthorWithDifferentIsbn() {
        when(bookRepository.findFirstByIsbnIgnoreCase("978-0-13-235088-4")).thenReturn(Optional.empty());
        when(bookRepository.findFirstByIsbnIgnoreCase("978-0-13-235088-5")).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse firstEdition = bookService.registerBook(
                new BookRegistrationRequest("978-0-13-235088-4", "Clean Code", "Robert C. Martin")
        );
        BookResponse secondEdition = bookService.registerBook(
                new BookRegistrationRequest("978-0-13-235088-5", "Clean Code", "Robert C. Martin")
        );

        assertThat(firstEdition.id()).isNotEqualTo(secondEdition.id());
        assertThat(firstEdition.isbn()).isEqualTo("978-0-13-235088-4");
        assertThat(secondEdition.isbn()).isEqualTo("978-0-13-235088-5");
    }

    @Test
    @DisplayName("Should list all books in the library")
    void shouldReturnAllBooks() {
        Book b1 = new Book("978-0-13-235088-4", "Clean Code", "Robert C. Martin");
        Book b2 = new Book("978-0-201-63361-0", "Design Patterns", "Erich Gamma");
        when(bookRepository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of(b1, b2));

        List<BookResponse> books = bookService.getAllBooks();

        assertThat(books).hasSize(2);
        assertThat(books.get(0).title()).isEqualTo("Clean Code");
        assertThat(books.get(1).title()).isEqualTo("Design Patterns");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when book ID does not exist")
    void shouldThrowExceptionWhenBookIdNotFound() {
        UUID bookId = UUID.randomUUID();
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(bookId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(bookId.toString());
    }
}

