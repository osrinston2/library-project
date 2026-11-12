package com.library.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for registering a new borrower")
public record BorrowerRegistrationRequest(

        @Schema(description = "Full name of the borrower", example = "Alice Tan")
        @NotBlank(message = "Borrower name is required")
        @Size(max = 255, message = "Borrower name must not exceed 255 characters")
        String name,

        @Schema(description = "Unique email address of the borrower", example = "alice.tan@example.com")
        @NotBlank(message = "Email address is required")
        @Email(message = "Email address must be a valid email format")
        @Size(max = 255, message = "Email address must not exceed 255 characters")
        String email
) {
}

