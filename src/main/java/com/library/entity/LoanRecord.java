package com.library.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "loan_records")
public class LoanRecord {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "borrower_id", nullable = false)
    private Borrower borrower;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LoanStatus status;

    @Column(name = "borrowed_at", nullable = false, updatable = false)
    private OffsetDateTime borrowedAt;

    @Column(name = "returned_at")
    private OffsetDateTime returnedAt;

    protected LoanRecord() {
    }

    public LoanRecord(UUID id, Book book, Borrower borrower) {
        this.id = id != null ? id : UUID.randomUUID();
        this.book = book;
        this.borrower = borrower;
        this.status = LoanStatus.ACTIVE;
        this.borrowedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public LoanRecord(Book book, Borrower borrower) {
        this(UUID.randomUUID(), book, borrower);
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.borrowedAt == null) {
            this.borrowedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
        if (this.status == null) {
            this.status = LoanStatus.ACTIVE;
        }
    }

    public void markReturned() {
        this.status = LoanStatus.RETURNED;
        this.returnedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Borrower getBorrower() {
        return borrower;
    }

    public void setBorrower(Borrower borrower) {
        this.borrower = borrower;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public OffsetDateTime getBorrowedAt() {
        return borrowedAt;
    }

    public void setBorrowedAt(OffsetDateTime borrowedAt) {
        this.borrowedAt = borrowedAt;
    }

    public OffsetDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(OffsetDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LoanRecord that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

