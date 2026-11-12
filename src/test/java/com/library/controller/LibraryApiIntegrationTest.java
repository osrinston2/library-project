package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.dto.BookRegistrationRequest;
import com.library.dto.BookResponse;
import com.library.dto.BorrowerRegistrationRequest;
import com.library.dto.BorrowerResponse;
import com.library.repository.BookRepository;
import com.library.repository.BorrowerRepository;
import com.library.repository.LoanRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanRecordRepository loanRecordRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowerRepository borrowerRepository;

    @BeforeEach
    void cleanDatabase() {
        loanRecordRepository.deleteAll();
        bookRepository.deleteAll();
        borrowerRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Register borrower: valid request returns 201, duplicate email returns 409, invalid input returns 400")
    void shouldRegisterBorrowerAndEnforceValidationAndUniqueness() throws Exception {
        BorrowerRegistrationRequest validRequest = new BorrowerRegistrationRequest("Alice Tan", "alice@example.com");

        mockMvc.perform(post("/api/v1/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Alice Tan"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        // Duplicate email -> 409 Conflict
        mockMvc.perform(post("/api/v1/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Invalid payload -> 400 Bad Request
        BorrowerRegistrationRequest invalidRequest = new BorrowerRegistrationRequest("", "not-an-email");
        mockMvc.perform(post("/api/v1/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("2. Register books: supports multiple copies of same ISBN with distinct IDs, enforces ISBN consistency, and lists all books")
    void shouldEnforceIsbnRulesAndListBooks() throws Exception {
        BookRegistrationRequest copy1Request = new BookRegistrationRequest(
                "978-0-13-235088-4", "Clean Code", "Robert C. Martin"
        );

        MvcResult copy1Result = mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(copy1Request)))
                .andExpect(status().isCreated())
                .andReturn();
        BookResponse copy1 = objectMapper.readValue(copy1Result.getResponse().getContentAsString(), BookResponse.class);

        // Register a second physical copy with the exact same ISBN, title, and author
        MvcResult copy2Result = mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(copy1Request)))
                .andExpect(status().isCreated())
                .andReturn();
        BookResponse copy2 = objectMapper.readValue(copy2Result.getResponse().getContentAsString(), BookResponse.class);

        assertThat(copy1.id()).isNotEqualTo(copy2.id());
        assertThat(copy1.isbn()).isEqualTo(copy2.isbn());

        // Register a book with same title and author, but a different ISBN -> allowed as different book
        BookRegistrationRequest differentIsbnRequest = new BookRegistrationRequest(
                "978-0-13-235088-9", "Clean Code", "Robert C. Martin"
        );
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(differentIsbnRequest)))
                .andExpect(status().isCreated());

        // Register a book with the same ISBN but different title -> 409 Conflict
        BookRegistrationRequest mismatchedTitleRequest = new BookRegistrationRequest(
                "978-0-13-235088-4", "Clean Architecture", "Robert C. Martin"
        );
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchedTitleRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // List all books -> should return all 3 registered copies
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("3. Borrow and Return lifecycle: prevents double borrowing of same bookId, allows borrowing another copy of same ISBN, and validates returns")
    void shouldManageBorrowAndReturnLifecycleCorrectly() throws Exception {
        BorrowerResponse alice = registerBorrower("Alice Tan", "alice@example.com");
        BorrowerResponse bob = registerBorrower("Bob Lim", "bob@example.com");

        BookResponse copy1 = registerBook("978-0-13-235088-4", "Clean Code", "Robert C. Martin");
        BookResponse copy2 = registerBook("978-0-13-235088-4", "Clean Code", "Robert C. Martin");

        // Alice borrows copy 1 -> 200 OK
        mockMvc.perform(post("/api/v1/borrowers/{borrowerId}/books/{bookId}/borrow", alice.id(), copy1.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(copy1.id().toString()))
                .andExpect(jsonPath("$.borrowerId").value(alice.id().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // Bob tries to borrow copy 1 while Alice has it -> 409 Conflict
        mockMvc.perform(post("/api/v1/borrowers/{borrowerId}/books/{bookId}/borrow", bob.id(), copy1.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // Bob borrows copy 2 (same ISBN, different bookId) -> 200 OK
        mockMvc.perform(post("/api/v1/borrowers/{borrowerId}/books/{bookId}/borrow", bob.id(), copy2.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookId").value(copy2.id().toString()))
                .andExpect(jsonPath("$.borrowerId").value(bob.id().toString()));

        // Bob tries to return copy 1 (which Alice borrowed) -> 409 Conflict
        mockMvc.perform(post("/api/v1/borrowers/{borrowerId}/books/{bookId}/return", bob.id(), copy1.id()))
                .andExpect(status().isConflict());

        // Alice returns copy 1 -> 200 OK
        mockMvc.perform(post("/api/v1/borrowers/{borrowerId}/books/{bookId}/return", alice.id(), copy1.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());

        // Alice tries to return copy 1 again when it is already returned -> 409 Conflict
        mockMvc.perform(post("/api/v1/borrowers/{borrowerId}/books/{bookId}/return", alice.id(), copy1.id()))
                .andExpect(status().isConflict());

        // Now that copy 1 is returned, Bob can borrow copy 1 -> 200 OK
        mockMvc.perform(post("/api/v1/borrowers/{borrowerId}/books/{bookId}/borrow", bob.id(), copy1.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.borrowerId").value(bob.id().toString()));
    }

    @Test
    @DisplayName("4. Concurrency guarantee: when multiple borrowers simultaneously request the same bookId, exactly one succeeds")
    void shouldAllowOnlyOneBorrowerWhenConcurrentRequestsTargetSameBook() throws Exception {
        int threadCount = 8;
        List<UUID> borrowerIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            BorrowerResponse borrower = registerBorrower("User " + i, "user" + i + "@example.com");
            borrowerIds.add(borrower.id());
        }

        BookResponse singleBook = registerBook("978-0-321-12521-7", "Domain-Driven Design", "Eric Evans");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (UUID borrowerId : borrowerIds) {
            tasks.add(() -> {
                readyLatch.countDown();
                startLatch.await();
                int status = mockMvc.perform(post("/api/v1/borrowers/{borrowerId}/books/{bookId}/borrow",
                                borrowerId, singleBook.id()))
                        .andReturn()
                        .getResponse()
                        .getStatus();
                if (status == 200) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    conflictCount.incrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(executor.submit(task));
        }

        readyLatch.await();
        startLatch.countDown();

        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1);
    }

    private BorrowerResponse registerBorrower(String name, String email) throws Exception {
        BorrowerRegistrationRequest req = new BorrowerRegistrationRequest(name, email);
        MvcResult res = mockMvc.perform(post("/api/v1/borrowers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), BorrowerResponse.class);
    }

    private BookResponse registerBook(String isbn, String title, String author) throws Exception {
        BookRegistrationRequest req = new BookRegistrationRequest(isbn, title, author);
        MvcResult res = mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), BookResponse.class);
    }
}

