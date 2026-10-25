package com.example.shopping.requestForQuotation.repository;

import com.example.shopping.requestForQuotation.entity.RfqQuoteRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RfqQuoteRoundRepository extends JpaRepository<RfqQuoteRound, UUID> {
    Optional<RfqQuoteRound> findTopByRfq_IdAndSupplier_IdOrderByRoundNumberDesc(
            UUID rfqId,
            UUID supplierId
    );
}
