package com.library.service;

import com.library.dto.BookRegistrationRequest;
import com.library.dto.BookResponse;
import com.library.entity.Book;
import com.library.exception.IsbnConflictException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional
    public BookResponse registerBook(BookRegistrationRequest request) {
        String normalizedIsbn = request.isbn().trim();
        String normalizedTitle = request.title().trim();
        String normalizedAuthor = request.author().trim();

        Optional<Book> existingBookOpt = bookRepository.findFirstByIsbnIgnoreCase(normalizedIsbn);
        if (existingBookOpt.isPresent()) {
            Book existingBook = existingBookOpt.get();
            if (!existingBook.getTitle().equals(normalizedTitle)
                    || !existingBook.getAuthor().equals(normalizedAuthor)) {
                throw new IsbnConflictException(String.format(
                        "ISBN '%s' is already registered with title '%s' and author '%s'. " +
                        "All copies sharing an ISBN must have the exact same title and author.",
                        existingBook.getIsbn(),
                        existingBook.getTitle(),
                        existingBook.getAuthor()
                ));
            }
            normalizedIsbn = existingBook.getIsbn();
        }

        Book book = new Book(normalizedIsbn, normalizedTitle, normalizedAuthor);
        Book saved = bookRepository.save(book);
        log.info("Registered new book copy with id={} and isbn={}", saved.getId(), saved.getIsbn());
        return BookResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(BookResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(UUID bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Book not found with id: %s", bookId)
                ));
        return BookResponse.fromEntity(book);
    }
}

