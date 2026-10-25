package com.example.shopping.requestForQuotation.dto.request;

import com.example.shopping.requestForQuotation.enums.RfqScoringCriterion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ScoreRequest(
        @NotNull
        RfqScoringCriterion criterion,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal score,

        @Size(max = 1000)
        String note
) {}
