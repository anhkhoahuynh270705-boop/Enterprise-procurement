package com.example.shopping.procurement.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.shopping.procurement.enums.ProcurementPriority;
import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.entity.ProcurementTicket;

@Repository
public interface ProcurementTicketRepository extends JpaRepository<ProcurementTicket, UUID> {

    @Query("SELECT DISTINCT t FROM ProcurementTicket t LEFT JOIN FETCH t.items")
    List<ProcurementTicket> findAllForDashboard();

    @Query("SELECT t FROM ProcurementTicket t WHERE (:status IS NULL OR t.status = :status) AND (:maker IS NULL OR t.makerUsername = :maker) ORDER BY t.createdAt DESC")
    List<ProcurementTicket> findVisibleTickets(@Param("status") ProcurementStatus status, @Param("maker") String maker);

    @Query("SELECT t FROM ProcurementTicket t WHERE (:status IS NULL OR t.status = :status) AND (:maker IS NULL OR t.makerUsername = :maker)")
    Page<ProcurementTicket> findVisibleTickets(@Param("status") ProcurementStatus status, @Param("maker") String maker, Pageable pageable);

    Optional<ProcurementTicket> findByTicketCode(String ticketCode);

    List<ProcurementTicket> findByStatus(ProcurementStatus status);

    List<ProcurementTicket> findByMakerUsername(String makerUsername);

    List<ProcurementTicket> findByStatusOrderByCreatedAtDesc(ProcurementStatus status);

    List<ProcurementTicket> findAllByOrderByCreatedAtDesc();

    Page<ProcurementTicket> findByStatusOrderByCreatedAtDesc(ProcurementStatus status, Pageable pageable);

    Page<ProcurementTicket> findByMakerUsername(String makerUsername, Pageable pageable);

    Page<ProcurementTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT t FROM ProcurementTicket t LEFT JOIN FETCH t.items WHERE t.id = :id")
    Optional<ProcurementTicket> findByIdWithItems(UUID id);

    @Query("SELECT COUNT(t) FROM ProcurementTicket t WHERE t.ticketCode LIKE :prefix%")
    long findMaxSequenceByTicketCodePrefix(String prefix);

    List<ProcurementTicket> findByDepartmentAndPriority(String department, ProcurementPriority priority);

    @Query("SELECT t FROM ProcurementTicket t WHERE t.id IN :ids")
    List<ProcurementTicket> findAllByIdIn(@Param("ids") Collection<UUID> ids);
}
