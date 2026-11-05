package com.example.shopping.purchaseorder.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.shopping.purchaseorder.entity.PurchaseOrder;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID>,
        JpaSpecificationExecutor<PurchaseOrder> {

    @EntityGraph(attributePaths = {"ticket", "supplier", "items"})
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"ticket", "supplier", "items"})
    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.orderCode LIKE CONCAT(:prefix, '%')")
    long countByOrderCodePrefix(@Param("prefix") String prefix);

    @Query("SELECT DISTINCT item.id FROM PurchaseOrderItem poi JOIN poi.procurementItem item " +
            "WHERE item.id IN :itemIds " +
            "AND poi.purchaseOrder.status <> PurchaseOrderStatus.CANCELLED " +
            "AND poi.purchaseOrder.status <> PurchaseOrderStatus.REJECTED")
    List<UUID> findAlreadyOrderedProcurementItemIds(@Param("itemIds") List<UUID> itemIds);

    @Query("SELECT DISTINCT item.id FROM PurchaseOrderItem poi JOIN poi.procurementItem item " +
            "WHERE poi.purchaseOrder.ticket.id = :ticketId " +
            "AND poi.purchaseOrder.status <> PurchaseOrderStatus.CANCELLED " +
            "AND poi.purchaseOrder.status <> PurchaseOrderStatus.REJECTED")
    List<UUID> findOrderedProcurementItemIdsByTicketId(@Param("ticketId") UUID ticketId);
}
