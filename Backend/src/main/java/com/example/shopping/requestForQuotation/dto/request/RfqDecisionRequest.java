package com.example.shopping.requestForQuotation.dto.request;

import com.example.shopping.requestForQuotation.enums.RfqReviewDecision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RfqDecisionRequest(
        @NotNull
        RfqReviewDecision decision,

        @NotBlank
        @Size(min = 10, max = 2000)
        String reason) {
}
