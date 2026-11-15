package com.library.controller;

import com.library.dto.BorrowerRegistrationRequest;
import com.library.dto.BorrowerResponse;
import com.library.dto.LoanResponse;
import com.library.service.BorrowerService;
import com.library.service.LoanService;
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
@RequestMapping("/api/v1/borrowers")
@Tag(name = "Borrowers & Loans", description = "Endpoints for registering borrowers and borrowing/returning books on their behalf")
public class BorrowerController {

    private final BorrowerService borrowerService;
    private final LoanService loanService;

    public BorrowerController(BorrowerService borrowerService, LoanService loanService) {
        this.borrowerService = borrowerService;
        this.loanService = loanService;
    }

    @PostMapping
    @Operation(summary = "Register a new borrower", description = "Registers new borrower with a unique email address.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Borrower registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "409", description = "Email address is already registered")
    })
    public ResponseEntity<BorrowerResponse> registerBorrower(
            @Valid @RequestBody BorrowerRegistrationRequest request) {
        BorrowerResponse response = borrowerService.registerBorrower(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all borrowers", description = "Returns a list of all registered borrowers.")
    @ApiResponse(responseCode = "200", description = "List of borrowers retrieved successfully")
    public ResponseEntity<List<BorrowerResponse>> getAllBorrowers() {
        return ResponseEntity.ok(borrowerService.getAllBorrowers());
    }

    @GetMapping("/{borrowerId}")
    @Operation(summary = "Get borrower by ID", description = "Retrieves a specific borrower by their unique ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Borrower retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Borrower not found")
    })
    public ResponseEntity<BorrowerResponse> getBorrowerById(@PathVariable UUID borrowerId) {
        return ResponseEntity.ok(borrowerService.getBorrowerById(borrowerId));
    }

    @PostMapping("/{borrowerId}/books/{bookId}/borrow")
    @Operation(
            summary = "Borrow a book on behalf of a borrower",
            description = "Checks out a specific book copy (by bookId) for the specified borrower. Only one member can borrow a book copy at a time."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book borrowed successfully"),
            @ApiResponse(responseCode = "404", description = "Borrower or Book not found"),
            @ApiResponse(responseCode = "409", description = "Book copy is already borrowed by a member")
    })
    public ResponseEntity<LoanResponse> borrowBook(
            @PathVariable UUID borrowerId,
            @PathVariable UUID bookId) {
        LoanResponse response = loanService.borrowBook(borrowerId, bookId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{borrowerId}/books/{bookId}/return")
    @Operation(
            summary = "Return a borrowed book on behalf of a borrower",
            description = "Returns a previously borrowed book copy on behalf of the borrower who checked it out."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Book returned successfully"),
            @ApiResponse(responseCode = "404", description = "Borrower or Book not found"),
            @ApiResponse(responseCode = "409", description = "Book is not currently borrowed or was borrowed by a different member")
    })
    public ResponseEntity<LoanResponse> returnBook(
            @PathVariable UUID borrowerId,
            @PathVariable UUID bookId) {
        LoanResponse response = loanService.returnBook(borrowerId, bookId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{borrowerId}/loans")
    @Operation(
            summary = "Get loan history for a borrower",
            description = "Returns active and historical loan records for the specified borrower."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan history retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Borrower not found")
    })
    public ResponseEntity<List<LoanResponse>> getBorrowerLoans(@PathVariable UUID borrowerId) {
        return ResponseEntity.ok(loanService.getBorrowerLoanHistory(borrowerId));
    }
}

