package com.example.shopping.procurement.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shopping.procurement.entity.ProcurementItem;

@Repository
public interface ProcurementItemRepository extends JpaRepository<ProcurementItem, UUID> {

    List<ProcurementItem> findByTicketId(UUID ticketId);

    void deleteByTicketId(UUID ticketId);
}
