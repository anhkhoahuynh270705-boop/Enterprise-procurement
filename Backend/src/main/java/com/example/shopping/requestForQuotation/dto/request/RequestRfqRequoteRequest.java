package com.example.shopping.requestForQuotation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequestRfqRequoteRequest(
        @NotEmpty
        List<@NotNull UUID> supplierIds,

        @NotBlank
        @Size(min = 5, max = 2000)
        String reason) {
}
