package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for registering a new book copy into the library")
public record BookRegistrationRequest(

        @Schema(description = "ISBN identifier of the book (ISBN-10 or ISBN-13)", example = "978-0-13-235088-4")
        @NotBlank(message = "ISBN is required")
        @Size(max = 32, message = "ISBN must not exceed 32 characters")
        @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "ISBN must contain only alphanumeric characters and hyphens")
        String isbn,

        @Schema(description = "Title of the book", example = "Clean Code")
        @NotBlank(message = "Book title is required")
        @Size(max = 255, message = "Book title must not exceed 255 characters")
        String title,

        @Schema(description = "Author of the book", example = "Robert C. Martin")
        @NotBlank(message = "Book author is required")
        @Size(max = 255, message = "Book author must not exceed 255 characters")
        String author
) {
}

