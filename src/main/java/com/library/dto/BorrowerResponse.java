package com.library.dto;

import com.library.entity.Borrower;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Registered borrower representation")
public record BorrowerResponse(
        @Schema(description = "Unique identifier of the borrower", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Full name of the borrower", example = "Alice Tan")
        String name,

        @Schema(description = "Email address of the borrower", example = "alice.tan@example.com")
        String email,

        @Schema(description = "Timestamp when the borrower was registered")
        OffsetDateTime createdAt
) {
    public static BorrowerResponse fromEntity(Borrower borrower) {
        return new BorrowerResponse(
                borrower.getId(),
                borrower.getName(),
                borrower.getEmail(),
                borrower.getCreatedAt()
        );
    }
}

