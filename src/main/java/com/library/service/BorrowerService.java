package com.library.service;

import com.library.dto.BorrowerRegistrationRequest;
import com.library.dto.BorrowerResponse;
import com.library.entity.Borrower;
import com.library.exception.DuplicateResourceException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.BorrowerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class BorrowerService {

    private static final Logger log = LoggerFactory.getLogger(BorrowerService.class);

    private final BorrowerRepository borrowerRepository;

    public BorrowerService(BorrowerRepository borrowerRepository) {
        this.borrowerRepository = borrowerRepository;
    }

    @Transactional
    public BorrowerResponse registerBorrower(BorrowerRegistrationRequest request) {
        String normalizedName = request.name().trim();
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (borrowerRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException(
                    String.format("A borrower with email '%s' is already registered.", normalizedEmail)
            );
        }

        Borrower borrower = new Borrower(normalizedName, normalizedEmail);
        Borrower saved = borrowerRepository.save(borrower);
        log.info("Registered new borrower with id={} and email={}", saved.getId(), saved.getEmail());
        return BorrowerResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public BorrowerResponse getBorrowerById(UUID borrowerId) {
        Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Borrower not found with id: %s", borrowerId)
                ));
        return BorrowerResponse.fromEntity(borrower);
    }

    @Transactional(readOnly = true)
    public List<BorrowerResponse> getAllBorrowers() {
        return borrowerRepository.findAll()
                .stream()
                .map(BorrowerResponse::fromEntity)
                .toList();
    }
}

