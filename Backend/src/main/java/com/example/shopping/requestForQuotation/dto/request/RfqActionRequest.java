package com.example.shopping.requestForQuotation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RfqActionRequest(
        @NotBlank
        @Size(min = 5, max = 2000)
        String reason
) {}
