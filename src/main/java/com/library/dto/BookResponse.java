package com.library.dto;

import com.library.entity.Book;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Registered book copy representation")
public record BookResponse(
        @Schema(description = "Unique identifier of this physical book copy", example = "7f8b9c1a-2d3e-4f5a-8b9c-0d1e2f3a4b5c")
        UUID id,

        @Schema(description = "ISBN number of the book", example = "978-0-13-235088-4")
        String isbn,

        @Schema(description = "Title of the book", example = "Clean Code")
        String title,

        @Schema(description = "Author of the book", example = "Robert C. Martin")
        String author,

        @Schema(description = "Whether this copy is currently borrowed", example = "false")
        boolean borrowed,

        @Schema(description = "ID of the borrower who currently holds this copy, or null if available")
        UUID borrowedByBorrowerId,

        @Schema(description = "Timestamp when this book copy was registered")
        OffsetDateTime createdAt
) {
    public static BookResponse fromEntity(Book book) {
        UUID borrowerId = book.getCurrentBorrower() != null ? book.getCurrentBorrower().getId() : null;
        return new BookResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.isBorrowed(),
                borrowerId,
                book.getCreatedAt()
        );
    }
}

