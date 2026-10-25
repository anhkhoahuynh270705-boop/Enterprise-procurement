package com.example.shopping.requestForQuotation.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RfqItemRequest(
        UUID procurementItemId,
        @Size(max = 50)
        String sku,

        @NotBlank
        @Size(max = 255)
        String itemName,

        @Size(max = 100)
        String category,

        @NotNull
        @Positive
        Integer quantity,

        @Size(max = 50)
        String unit,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal budgetUnitPrice
) {}
