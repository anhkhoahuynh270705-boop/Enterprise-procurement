package com.example.shopping.requestForQuotation.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteItemResponse(
        UUID rfqItemId,
        boolean quoted,
        BigDecimal unitPrice,
        String note) {
}