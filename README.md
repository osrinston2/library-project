# Library Management System RESTful API

A RESTful API built with **Java 17** and **Spring Boot Framework** to manage a library catalog, borrowers, and book loans.

---

## Table of Contents

1. [Features & Architecture](#1-features--architecture)
2. [Database Choice & Justification](#2-database-choice--justification)
3. [Data Models & Domain Rules](#3-data-models--domain-rules)
4. [Documented Assumptions](#4-documented-assumptions)
5. [12-Factor App Conformance](#5-12-factor-app-conformance)
6. [Getting Started](#6-getting-started)
7. [API Documentation](#7-api-documentation)
8. [Error Handling & Validation](#8-error-handling--validation)
9. [Testing & CI/CD](#9-testing--cicd)

---

## 1. Features & Architecture

- **Register a new borrower** (`POST /api/v1/borrowers`) with unique email validation.
- **Register a new book copy** (`POST /api/v1/books`) enforcing ISBN title/author consistency while supporting multiple physical copies with distinct IDs.
- **List all books** (`GET /api/v1/books`) with real-time availability status and current holder reference.
- **Borrow a book copy on behalf of a borrower** (`POST /api/v1/borrowers/{borrowerId}/books/{bookId}/borrow`) protected by JPA `@Version` optimistic locking to guarantee no two borrowers can check out the same `bookId` simultaneously.
- **Return a borrowed book** (`POST /api/v1/borrowers/{borrowerId}/books/{bookId}/return`) recording return timestamps in the loan audit history.
- **Interactive OpenAPI / Swagger UI** available out of the box at `/swagger-ui/index.html`.

### Layered Architecture

```
src/main/java/com/library
├── config/       # OpenAPI / Swagger configuration
├── controller/   # REST controllers (HTTP request/response mapping & validation)
├── dto/          # Immutable Java 17 records for API requests, responses, and errors
├── entity/       # JPA domain entities (Borrower, Book, LoanRecord, LoanStatus)
├── exception/    # Domain exceptions and centralized @RestControllerAdvice handler
├── repository/   # Spring Data JPA repositories
└── service/      # Transactional business logic (BorrowerService, BookService, LoanService)
```

---

## 2. Database Choice & Justification

### Primary Database: **PostgreSQL 16**

#### Why Relational (RDBMS) over NoSQL?
1. **Strict ACID Transactions for Borrowing Concurrency**:
   - The core invariant of the library system is that **no more than one member may borrow the same physical book (`bookId`) at a time**.
   - PostgreSQL provides full ACID transaction guarantees and row-level concurrency control. Combined with JPA `@Version` optimistic locking on the `books` table, concurrent requests attempting to borrow the same `bookId` are deterministically serialized: one transaction commits and increments the `version` column, while competing transactions immediately fail with an optimistic lock conflict (`409 Conflict`).
2. **Referential Integrity**:
   - Relationships between `borrowers`, `books`, and `loan_records` are strictly enforced via foreign key constraints (`fk_books_current_borrower`, `fk_loan_records_book`, `fk_loan_records_borrower`) and unique constraints (`uq_borrowers_email`).
3. **Structured Schema & Versioned Migrations**:
   - Flyway (`src/main/resources/db/migration/V1__init_schema.sql`) manages deterministic schema evolution across all environments.
4. **Developer Experience & Test Portability**:
   - While PostgreSQL is used for production and Docker Compose deployments, an H2 in-memory profile (`local` and `test` profiles running in `MODE=PostgreSQL`) is included so automated test suites (`mvn test`) and quick local evaluations can execute immediately without requiring an external database server.

---

## 3. Data Models & Domain Rules

### `Borrower` (`borrowers` table)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique identifier of the borrower |
| `name` | `VARCHAR(255)` | `NOT NULL` | Full name of the borrower |
| `email` | `VARCHAR(255)` | `UNIQUE`, `NOT NULL` | Normalized (lowercase) email address |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | Registration timestamp (UTC) |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | Last update timestamp (UTC) |

### `Book` (`books` table)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique ID of this specific physical book copy |
| `isbn` | `VARCHAR(32)` | `NOT NULL`, Indexed | ISBN identifier of the edition |
| `title` | `VARCHAR(255)` | `NOT NULL` | Book title |
| `author` | `VARCHAR(255)` | `NOT NULL` | Book author |
| `borrowed` | `BOOLEAN` | `NOT NULL`, Default `false` | Whether this copy is currently checked out |
| `current_borrower_id` | `UUID` | `FOREIGN KEY (borrowers.id)`, Nullable | Current borrower holding this copy |
| `version` | `BIGINT` | `NOT NULL`, `@Version` | Optimistic locking version counter |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | Registration timestamp (UTC) |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL` | Last update timestamp (UTC) |

### `LoanRecord` (`loan_records` table)
| Field | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | `PRIMARY KEY`, `NOT NULL` | Unique identifier of the loan record |
| `book_id` | `UUID` | `FOREIGN KEY (books.id)`, `NOT NULL` | Borrowed physical book copy |
| `borrower_id` | `UUID` | `FOREIGN KEY (borrowers.id)`, `NOT NULL` | Borrower who checked out the book |
| `status` | `VARCHAR(32)` | `NOT NULL` (`ACTIVE`, `RETURNED`) | Lifecycle state of the loan |
| `borrowed_at` | `TIMESTAMPTZ` | `NOT NULL` | Checkout timestamp (UTC) |
| `returned_at` | `TIMESTAMPTZ` | Nullable | Return timestamp (UTC) |

---

## 4. Documented Assumptions

To address any requirements not explicitly specified in the problem statement, the following engineering assumptions were made:

1. **Physical Copy Identity vs. ISBN Edition Identity**:
   - Each call to `POST /api/v1/books` registers **one physical copy** in the library and generates a new unique `UUID` (`id`).
   - Registering multiple copies with the same `isbn` is permitted **only if** both `title` and `author` match existing records of that `isbn` (after trimming leading/trailing whitespace). If either `title` or `author` differs for an existing `isbn`, the API rejects the request with `409 Conflict`.
   - Two books with the exact same `title` and `author` but **different** `isbn` values (e.g., hardcover vs. paperback or 1st vs. 2nd edition) are treated as different books and are permitted.
2. **Borrower Uniqueness**:
   - A borrower's `email` address uniquely identifies their membership account (case-insensitively). Attempting to register a second borrower with the same email returns `409 Conflict`.
   - Two different borrowers may share the same `name` as long as their `email` addresses are distinct.
3. **Borrowing Rules**:
   - A borrower may borrow multiple different book copies simultaneously (including two distinct copies of the same ISBN, if desired), as no upper limit per borrower was specified.
   - Only one borrower can hold a specific physical book copy (`bookId`) at any given time. Attempting to borrow an already-borrowed `bookId` returns `409 Conflict`.
4. **Returning Rules**:
   - Only the borrower who currently holds an active loan for a given `bookId` can return that book via `POST /api/v1/borrowers/{borrowerId}/books/{bookId}/return`.
   - Attempting to return a book that is currently available (`borrowed = false`), or attempting to return a book on behalf of a borrower who did not check it out, returns `409 Conflict`.
5. **Authentication / Authorization**:
   - Because the specification states *"The API should allow API user to perform the following actions on behalf of a borrower"*, the API is designed as an internal/service-level or librarian-operated REST API where `borrowerId` is passed explicitly in the endpoint path (`/api/v1/borrowers/{borrowerId}/books/{bookId}/borrow`), without requiring OAuth/JWT bearer tokens.
6. **ISBN Format Validation**:
   - ISBN strings are trimmed and validated to allow alphanumeric characters and hyphens (`^[A-Za-z0-9-]+$`, up to 32 characters) so both standard ISBN-10 / ISBN-13 formats (e.g., `978-0-13-235088-4`, `0132350882`) and custom catalog ISBNs are supported cleanly.

---

## 5. 12-Factor App Conformance

| Factor | Principle | Implementation in This Project |
| :--- | :--- | :--- |
| **I. Codebase** | One codebase tracked in revision control, many deploys | Single Git repository with declarative build & environment configurations. |
| **II. Dependencies** | Explicitly declare and isolate dependencies | Managed declaratively in `pom.xml` with Maven Wrapper (`mvnw` / `mvnw.cmd`). |
| **III. Config** | Store config in the environment | All environment-varying settings (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SERVER_PORT`, `LOG_LEVEL_APP`) are externalized in `application.yml` with `.env.example` reference. |
| **IV. Backing Services** | Treat backing services as attached resources | PostgreSQL database is attached purely via JDBC URL & credential environment variables; swappable without code changes. |
| **V. Build, Release, Run** | Strictly separate build and run stages | Multi-stage `Dockerfile` separates the Maven compilation stage (`builder`) from the immutable JRE 17 runtime container (`runtime`). |
| **VI. Processes** | Execute the app as one or more stateless processes | Application tier is completely stateless; all state is persisted in PostgreSQL. |
| **VII. Port Binding** | Export services via port binding | Embedded Tomcat binds directly to `${SERVER_PORT:8080}`. |
| **VIII. Concurrency** | Scale out via the process model | Stateless API + database-backed `@Version` optimistic locking allows horizontal scaling across multiple container replicas safely. |
| **IX. Disposability** | Maximize robustness with fast startup and graceful shutdown | Configured with `server.shutdown: graceful` and `timeout-per-shutdown-phase: 20s` to finish in-flight requests on `SIGTERM`. |
| **X. Dev/Prod Parity** | Keep development, staging, and production as similar as possible | `docker-compose.yml` runs PostgreSQL 16 + Flyway migrations identical to production. |
| **XI. Logs** | Treat logs as event streams | Structured ISO-8601 UTC logs are written strictly to `stdout` (`console`) with no local log file management inside the container. |
| **XII. Admin Processes** | Run admin/management tasks as one-off processes | Database schema migrations run automatically and idempotently via Flyway on startup. |

---

## 6. Getting Started

### Prerequisites
- **Option A (Docker Compose)**: Docker & Docker Compose
- **Option B (Local Java)**: JDK 17+ and Maven 3.9+ (or use included `./mvnw` / `mvnw.cmd`)

### Option A: Run with Docker Compose (PostgreSQL + API)

```bash
docker compose up --build
```
- The API will be available at `http://localhost:8081`
- Swagger UI will be available at `http://localhost:8081/swagger-ui.html`
- Health check endpoint: `http://localhost:8081/actuator/health`

### Option B: Run Locally with H2 In-Memory Profile (Zero External Setup)

```bash
# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Windows PowerShell (quote -D arguments containing dots, or use -Plocal)
.\mvnw.cmd spring-boot:run -Plocal
# or:
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### Option C: Run Locally against PostgreSQL

*(Requires a running PostgreSQL instance on `localhost:5432`, e.g., started via `docker compose up -d postgres`)*

```powershell
# Windows PowerShell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/library_db"
$env:SPRING_DATASOURCE_USERNAME="library_user"
$env:SPRING_DATASOURCE_PASSWORD="library_pass"
.\mvnw.cmd spring-boot:run
```

```bash
# Linux / macOS (Bash / Zsh)
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/library_db
export SPRING_DATASOURCE_USERNAME=library_user
export SPRING_DATASOURCE_PASSWORD=library_pass
./mvnw spring-boot:run
```

### Pre-Seeded Sample Data (`V2__seed_sample_data.sql`)

On startup, Flyway automatically seeds the database with sample borrowers, books (including multiple copies of the same ISBN), and loan records using fixed UUIDs so you can test every endpoint immediately:

#### Pre-Seeded Borrowers
| Borrower ID | Name | Email |
| :--- | :--- | :--- |
| `11111111-1111-1111-1111-111111111111` | Alice Tan | `alice.tan@example.com` |
| `22222222-2222-2222-2222-222222222222` | Bob Lim | `bob.lim@example.com` |
| `33333333-3333-3333-3333-333333333333` | Charlie Wong | `charlie.wong@example.com` |

#### Pre-Seeded Book Copies
| Book ID | ISBN | Title | Author | Borrowed? | Borrowed By |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1` | `978-0-13-235088-4` | Clean Code | Robert C. Martin | `false` (Available) | — |
| `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2` | `978-0-13-235088-4` | Clean Code | Robert C. Martin | `true` (Borrowed) | Alice Tan (`11111111-...`) |
| `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1` | `978-1-4493-7332-0` | Designing Data-Intensive Applications | Martin Kleppmann | `false` (Available) | — |
| `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2` | `978-1-4493-7332-0` | Designing Data-Intensive Applications | Martin Kleppmann | `false` (Available) | — |
| `cccccccc-cccc-cccc-cccc-ccccccccccc1` | `978-0-13-468599-1` | Effective Java | Joshua Bloch | `false` (Available) | — |

#### Quick Instant Test Commands (Using Pre-Seeded IDs)
*(Note: On Windows PowerShell, use `curl.exe` instead of `curl` so PowerShell calls the real cURL binary rather than its `Invoke-WebRequest` alias.)*
```powershell
# 1. List all pre-seeded books
curl.exe -X GET http://localhost:8081/api/v1/books

# 2. Borrow Available Copy 1 of Clean Code on behalf of Bob Lim -> 200 OK
curl.exe -X POST http://localhost:8081/api/v1/borrowers/22222222-2222-2222-2222-222222222222/books/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1/borrow

# 3. Try borrowing Already-Borrowed Copy 2 of Clean Code on behalf of Charlie Wong -> 409 Conflict
curl.exe -X POST http://localhost:8081/api/v1/borrowers/33333333-3333-3333-3333-333333333333/books/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2/borrow

# 4. Return Copy 2 of Clean Code on behalf of Alice Tan -> 200 OK
curl.exe -X POST http://localhost:8081/api/v1/borrowers/11111111-1111-1111-1111-111111111111/books/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2/return
```

---

## 7. API Documentation

Once the server is running, interactive OpenAPI documentation is available at:
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON Spec**: `http://localhost:8081/api-docs`

### Summary of Endpoints

| Action | HTTP Method | Endpoint | Success Status |
| :--- | :--- | :--- | :--- |
| **1. Register a new borrower** | `POST` | `/api/v1/borrowers` | `201 Created` |
| **2. Register a new book** | `POST` | `/api/v1/books` | `201 Created` |
| **3. Get a list of all books** | `GET` | `/api/v1/books` | `200 OK` |
| **4. Borrow a book by book ID** | `POST` | `/api/v1/borrowers/{borrowerId}/books/{bookId}/borrow` | `200 OK` |
| **5. Return a borrowed book** | `POST` | `/api/v1/borrowers/{borrowerId}/books/{bookId}/return` | `200 OK` |
| Get borrower by ID | `GET` | `/api/v1/borrowers/{borrowerId}` | `200 OK` |
| List all borrowers | `GET` | `/api/v1/borrowers` | `200 OK` |
| Get borrower loan history | `GET` | `/api/v1/borrowers/{borrowerId}/loans` | `200 OK` |
| Get book by ID | `GET` | `/api/v1/books/{bookId}` | `200 OK` |

---

### Detailed Endpoint Walkthrough

#### 1. Register a New Borrower
- **Endpoint**: `POST /api/v1/borrowers`
- **Request Body**:
```bash
curl -X POST http://localhost:8081/api/v1/borrowers \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Tan",
    "email": "alice.tan@example.com"
  }'
```
- **Response (`201 Created`)**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Alice Tan",
  "email": "alice.tan@example.com",
  "createdAt": "2026-11-13T00:30:00Z"
}
```

#### 2. Register a New Book Copy
- **Endpoint**: `POST /api/v1/books`
- **Request Body**:
```bash
curl -X POST http://localhost:8081/api/v1/books \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "978-0-13-235088-4",
    "title": "Clean Code",
    "author": "Robert C. Martin"
  }'
```
- **Response (`201 Created`)**:
```json
{
  "id": "7f8b9c1a-2d3e-4f5a-8b9c-0d1e2f3a4b5c",
  "isbn": "978-0-13-235088-4",
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "borrowed": false,
  "borrowedByBorrowerId": null,
  "createdAt": "2026-11-13T00:31:00Z"
}
```

#### 3. Get a List of All Books in the Library
- **Endpoint**: `GET /api/v1/books`
```bash
curl -X GET http://localhost:8081/api/v1/books
```
- **Response (`200 OK`)**:
```json
[
  {
    "id": "7f8b9c1a-2d3e-4f5a-8b9c-0d1e2f3a4b5c",
    "isbn": "978-0-13-235088-4",
    "title": "Clean Code",
    "author": "Robert C. Martin",
    "borrowed": false,
    "borrowedByBorrowerId": null,
    "createdAt": "2026-11-13T00:31:00Z"
  }
]
```

#### 4. Borrow a Book on Behalf of a Borrower
- **Endpoint**: `POST /api/v1/borrowers/{borrowerId}/books/{bookId}/borrow`
```bash
curl -X POST http://localhost:8081/api/v1/borrowers/550e8400-e29b-41d4-a716-446655440000/books/7f8b9c1a-2d3e-4f5a-8b9c-0d1e2f3a4b5c/borrow
```
- **Response (`200 OK`)**:
```json
{
  "loanId": "c9a646d3-9c61-4cb7-bfcd-ee2522c8f633",
  "bookId": "7f8b9c1a-2d3e-4f5a-8b9c-0d1e2f3a4b5c",
  "isbn": "978-0-13-235088-4",
  "bookTitle": "Clean Code",
  "borrowerId": "550e8400-e29b-41d4-a716-446655440000",
  "borrowerName": "Alice Tan",
  "status": "ACTIVE",
  "borrowedAt": "2026-11-13T00:35:00Z",
  "returnedAt": null
}
```

#### 5. Return a Borrowed Book on Behalf of a Borrower
- **Endpoint**: `POST /api/v1/borrowers/{borrowerId}/books/{bookId}/return`
```bash
curl -X POST http://localhost:8081/api/v1/borrowers/550e8400-e29b-41d4-a716-446655440000/books/7f8b9c1a-2d3e-4f5a-8b9c-0d1e2f3a4b5c/return
```
- **Response (`200 OK`)**:
```json
{
  "loanId": "c9a646d3-9c61-4cb7-bfcd-ee2522c8f633",
  "bookId": "7f8b9c1a-2d3e-4f5a-8b9c-0d1e2f3a4b5c",
  "isbn": "978-0-13-235088-4",
  "bookTitle": "Clean Code",
  "borrowerId": "550e8400-e29b-41d4-a716-446655440000",
  "borrowerName": "Alice Tan",
  "status": "RETURNED",
  "borrowedAt": "2026-11-13T00:35:00Z",
  "returnedAt": "2026-11-13T00:40:00Z"
}
```

---

## 8. Error Handling & Validation

All errors return a consistent JSON structure via `GlobalExceptionHandler`:

### Validation Error Example (`400 Bad Request`)
```json
{
  "timestamp": "2026-11-13T00:36:12.104Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/v1/borrowers",
  "validationErrors": {
    "name": "Borrower name is required",
    "email": "Email address must be a valid email format"
  }
}
```

### Business Conflict Error Example (`409 Conflict`)
Returned when:
- A borrower registers with an email that already exists.
- A book is registered with an existing ISBN but a mismatched title or author.
- A borrower tries to borrow a book copy (`bookId`) that is already borrowed.
- A borrower tries to return a book that is not currently borrowed or was borrowed by someone else.

```json
{
  "timestamp": "2026-11-13T00:37:00.512Z",
  "status": 409,
  "error": "Conflict",
  "message": "Book with id '7f8b9c1a-2d3e-4f5a-8b9c-0d1e2f3a4b5c' is already borrowed and unavailable.",
  "path": "/api/v1/borrowers/550e8400-e29b-41d4-a716-446655440000/books/7f8b9c1a-2d3e-4f5a-8b9c-0d1e2f3a4b5c/borrow"
}
```

---

## 9. Testing & CI/CD

### Run Unit & Integration Tests
```bash
# Linux / macOS
./mvnw clean verify

# Windows
.\mvnw.cmd clean verify
```
- **Unit Tests**: `BorrowerServiceTest`, `BookServiceTest`, `LoanServiceTest` (JUnit 5 + Mockito + AssertJ).
- **Integration & Concurrency Tests**: `LibraryApiIntegrationTest` (`@SpringBootTest` + `MockMvc` + multi-threaded concurrent checkout contention test).
- **Code Coverage**: JaCoCo HTML report is generated automatically at `target/site/jacoco/index.html`.
- **CI/CD**: Declarative GitHub Actions pipeline in `.github/workflows/ci.yml` executes tests, uploads coverage artifacts, and verifies the multi-stage Docker build on every push and pull request.

