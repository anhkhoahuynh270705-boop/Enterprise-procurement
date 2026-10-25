package com.example.shopping.requestForQuotation.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ItemAwardResponse(
        UUID rfqItemId,
        UUID supplierId,
        String supplierName,
        String reason,
        String selectedBy,
        LocalDateTime selectedAt) {
}