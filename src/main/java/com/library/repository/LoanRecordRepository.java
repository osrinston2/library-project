package com.library.repository;

import com.library.entity.LoanRecord;
import com.library.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoanRecordRepository extends JpaRepository<LoanRecord, UUID> {

    Optional<LoanRecord> findFirstByBookIdAndStatusOrderByBorrowedAtDesc(UUID bookId, LoanStatus status);

    List<LoanRecord> findByBorrowerIdOrderByBorrowedAtDesc(UUID borrowerId);
}

