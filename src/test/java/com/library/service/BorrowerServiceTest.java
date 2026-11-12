package com.library.service;

import com.library.dto.BorrowerRegistrationRequest;
import com.library.dto.BorrowerResponse;
import com.library.entity.Borrower;
import com.library.exception.DuplicateResourceException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.BorrowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    private BorrowerService borrowerService;

    @BeforeEach
    void setUp() {
        borrowerService = new BorrowerService(borrowerRepository);
    }

    @Test
    @DisplayName("Should register a new borrower with normalized name and lowercase email")
    void shouldRegisterNewBorrowerSuccessfully() {
        BorrowerRegistrationRequest request = new BorrowerRegistrationRequest("  Alice Tan  ", "  Alice.Tan@Example.com ");
        when(borrowerRepository.existsByEmailIgnoreCase("alice.tan@example.com")).thenReturn(false);
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerResponse response = borrowerService.registerBorrower(request);

        ArgumentCaptor<Borrower> captor = ArgumentCaptor.forClass(Borrower.class);
        verify(borrowerRepository).save(captor.capture());

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Alice Tan");
        assertThat(response.email()).isEqualTo("alice.tan@example.com");
        assertThat(captor.getValue().getEmail()).isEqualTo("alice.tan@example.com");
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email is already registered")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        BorrowerRegistrationRequest request = new BorrowerRegistrationRequest("Alice Tan", "alice@example.com");
        when(borrowerRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> borrowerService.registerBorrower(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("alice@example.com");

        verify(borrowerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return borrower when found by ID")
    void shouldReturnBorrowerById() {
        UUID id = UUID.randomUUID();
        Borrower borrower = new Borrower(id, "Bob Lim", "bob@example.com");
        when(borrowerRepository.findById(id)).thenReturn(Optional.of(borrower));

        BorrowerResponse response = borrowerService.getBorrowerById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Bob Lim");
        assertThat(response.email()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when borrower ID does not exist")
    void shouldThrowExceptionWhenBorrowerIdNotFound() {
        UUID id = UUID.randomUUID();
        when(borrowerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowerService.getBorrowerById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    @DisplayName("Should return all registered borrowers")
    void shouldReturnAllBorrowers() {
        Borrower b1 = new Borrower("Alice", "alice@example.com");
        Borrower b2 = new Borrower("Bob", "bob@example.com");
        when(borrowerRepository.findAll()).thenReturn(List.of(b1, b2));

        List<BorrowerResponse> responses = borrowerService.getAllBorrowers();

        assertThat(responses).hasSize(2);
    }
}

