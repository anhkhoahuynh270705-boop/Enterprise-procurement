package com.example.shopping.purchaseorder.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;

@Builder
public record PurchaseOrderItemResponse(
        UUID id,
        UUID procurementItemId,
        String itemCode,
        String itemName,
        String category,
        Integer quantity,
        String unit,
        BigDecimal unitPrice,
        BigDecimal totalPrice) implements Serializable {
}
