package com.example.shopping.requestForQuotation.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ItemWinnerRequest(
        @NotNull
        UUID rfqItemId,

        @NotNull
        UUID supplierId,

        @Size(max = 2000)
        String reason
) {}
