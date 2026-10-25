package com.example.shopping.requestForQuotation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        UUID procurementItemId,
        String sku,
        String itemName,
        String category,
        int quantity,
        String unit,
        BigDecimal budgetUnitPrice,
        boolean active) {
}