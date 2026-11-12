package com.library.dto;

import com.library.entity.LoanRecord;
import com.library.entity.LoanStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Book borrowing / return loan record representation")
public record LoanResponse(
        @Schema(description = "Unique identifier of the loan transaction")
        UUID loanId,

        @Schema(description = "Unique identifier of the book copy")
        UUID bookId,

        @Schema(description = "ISBN number of the book")
        String isbn,

        @Schema(description = "Title of the book")
        String bookTitle,

        @Schema(description = "Unique identifier of the borrower")
        UUID borrowerId,

        @Schema(description = "Name of the borrower")
        String borrowerName,

        @Schema(description = "Current status of the loan (ACTIVE or RETURNED)")
        LoanStatus status,

        @Schema(description = "Timestamp when the book was borrowed")
        OffsetDateTime borrowedAt,

        @Schema(description = "Timestamp when the book was returned, or null if still active")
        OffsetDateTime returnedAt
) {
    public static LoanResponse fromEntity(LoanRecord loanRecord) {
        return new LoanResponse(
                loanRecord.getId(),
                loanRecord.getBook().getId(),
                loanRecord.getBook().getIsbn(),
                loanRecord.getBook().getTitle(),
                loanRecord.getBorrower().getId(),
                loanRecord.getBorrower().getName(),
                loanRecord.getStatus(),
                loanRecord.getBorrowedAt(),
                loanRecord.getReturnedAt()
        );
    }
}

