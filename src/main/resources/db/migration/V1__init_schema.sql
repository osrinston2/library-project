CREATE TABLE borrowers (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_borrowers PRIMARY KEY (id),
    CONSTRAINT uq_borrowers_email UNIQUE (email)
);

CREATE TABLE books (
    id UUID NOT NULL,
    isbn VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    borrowed BOOLEAN NOT NULL DEFAULT FALSE,
    current_borrower_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_books PRIMARY KEY (id),
    CONSTRAINT fk_books_current_borrower FOREIGN KEY (current_borrower_id) REFERENCES borrowers (id)
);

CREATE INDEX idx_books_isbn ON books (isbn);

CREATE TABLE loan_records (
    id UUID NOT NULL,
    book_id UUID NOT NULL,
    borrower_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    borrowed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    returned_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_loan_records PRIMARY KEY (id),
    CONSTRAINT fk_loan_records_book FOREIGN KEY (book_id) REFERENCES books (id),
    CONSTRAINT fk_loan_records_borrower FOREIGN KEY (borrower_id) REFERENCES borrowers (id)
);

CREATE INDEX idx_loan_records_book_status ON loan_records (book_id, status);
CREATE INDEX idx_loan_records_borrower_status ON loan_records (borrower_id, status);

