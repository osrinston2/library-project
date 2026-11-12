package com.library.controller;

import com.library.dto.BookRegistrationRequest;
import com.library.dto.BookResponse;
import com.library.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books")
@Tag(name = "Books", description = "Endpoints for registering and listing books in the library")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @Operation(
            summary = "Register a new book copy",
            description = "Registers a new physical book copy. Multiple copies with the same ISBN are allowed and receive distinct IDs, provided their title and author match existing copies of that ISBN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Book copy registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "409", description = "ISBN conflict: another book with the same ISBN has a different title or author")
    })
    public ResponseEntity<BookResponse> registerBook(
            @Valid @RequestBody BookRegistrationRequest request) {
        BookResponse response = bookService.registerBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Get all books in the library",
            description = "Retrieves a list of all registered physical book copies and their current availability."
    )
    @ApiResponse(responseCode = "200", description = "List of books retrieved successfully")
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        return ResponseEntity.ok(bookService.getAllBooks());
    }

    @GetMapping("/{bookId}")
    @Operation(
            summary = "Get a book copy by ID",
            description = "Retrieves details of a specific physical book copy by its unique ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book copy retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Book not found")
    })
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID bookId) {
        return ResponseEntity.ok(bookService.getBookById(bookId));
    }
}

