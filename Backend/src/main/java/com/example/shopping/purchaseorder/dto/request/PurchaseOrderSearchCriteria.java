package com.example.shopping.purchaseorder.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;

public record PurchaseOrderSearchCriteria(
        String keyword,
        PurchaseOrderStatus status,
        UUID supplierId,
        String ticket,
        String orderCode,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount,
        LocalDate createdFrom,
        LocalDate createdTo,
        LocalDate deliveryFrom,
        LocalDate deliveryTo) {
}
