package com.example.shopping.purchaseorder.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;

import lombok.Builder;

@Builder
public record PurchaseOrderResponse(
        UUID id,
        String orderCode,
        UUID ticketId,
        String ticketCode,
        String ticketTitle,
        UUID supplierId,
        String supplierCode,
        String supplierName,
        PurchaseOrderStatus status,
        LocalDate expectedDeliveryDate,
        String notes,
        BigDecimal totalAmount,
        String currency,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewComment,
        List<PurchaseOrderItemResponse> items) {
}
