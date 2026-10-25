package com.example.shopping.requestForQuotation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record QuoteItemRequest(
        @NotNull
        UUID rfqItemId,

        @NotNull
        Boolean quoted,

        @DecimalMin("0.00")
        BigDecimal unitPrice,

        @Size(max = 1000)
        String note
) {}
