package com.example.shopping.requestForQuotation.dto.request.supplierrequest;

import com.example.shopping.requestForQuotation.dto.request.SubmitRfqQuoteRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupplierQuoteSubmissionRequest(
        @NotBlank
        String token,
        @NotNull
        @Valid
        SubmitRfqQuoteRequest quote
) {
}
