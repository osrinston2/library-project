-- =====================================================================
-- V2: Seed sample borrowers, books (including multiple copies), and loans
-- =====================================================================

-- 1. Sample Borrowers
INSERT INTO borrowers (id, name, email, created_at, updated_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Alice Tan', 'alice.tan@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'Bob Lim', 'bob.lim@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333333', 'Charlie Wong', 'charlie.wong@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. Sample Books (Multiple copies of the same ISBN have distinct IDs)
INSERT INTO books (id, isbn, title, author, borrowed, current_borrower_id, version, created_at, updated_at) VALUES
    -- Clean Code (Copy 1 - Available)
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', '978-0-13-235088-4', 'Clean Code', 'Robert C. Martin', FALSE, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Clean Code (Copy 2 - Currently borrowed by Alice Tan)
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '978-0-13-235088-4', 'Clean Code', 'Robert C. Martin', TRUE, '11111111-1111-1111-1111-111111111111', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Designing Data-Intensive Applications (Copy 1 - Available)
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', '978-1-4493-7332-0', 'Designing Data-Intensive Applications', 'Martin Kleppmann', FALSE, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Designing Data-Intensive Applications (Copy 2 - Available)
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2', '978-1-4493-7332-0', 'Designing Data-Intensive Applications', 'Martin Kleppmann', FALSE, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Effective Java (Copy 1 - Available)
    ('cccccccc-cccc-cccc-cccc-ccccccccccc1', '978-0-13-468599-1', 'Effective Java', 'Joshua Bloch', FALSE, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. Sample Loan Records
INSERT INTO loan_records (id, book_id, borrower_id, status, borrowed_at, returned_at) VALUES
    -- Active loan: Alice Tan currently borrowing Clean Code (Copy 2)
    ('dddddddd-dddd-dddd-dddd-ddddddddddd1', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2', '11111111-1111-1111-1111-111111111111', 'ACTIVE', CURRENT_TIMESTAMP, NULL),
    -- Historical returned loan: Bob Lim previously borrowed and returned Effective Java (Copy 1)
    ('dddddddd-dddd-dddd-dddd-ddddddddddd2', 'cccccccc-cccc-cccc-cccc-ccccccccccc1', '22222222-2222-2222-2222-222222222222', 'RETURNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

