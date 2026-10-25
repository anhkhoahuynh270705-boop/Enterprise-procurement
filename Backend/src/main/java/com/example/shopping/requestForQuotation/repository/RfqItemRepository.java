package com.example.shopping.requestForQuotation.repository;

import com.example.shopping.requestForQuotation.entity.RfqItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RfqItemRepository extends JpaRepository<RfqItem, UUID> {
    List<RfqItem> findByRfq_IdAndActiveTrue(UUID rfqId);
}
