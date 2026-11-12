package com.library.repository;

import com.library.entity.Borrower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BorrowerRepository extends JpaRepository<Borrower, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Borrower> findByEmailIgnoreCase(String email);
}

