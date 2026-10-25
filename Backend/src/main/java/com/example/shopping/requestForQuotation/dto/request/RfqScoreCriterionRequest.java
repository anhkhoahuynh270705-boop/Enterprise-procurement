package com.example.shopping.requestForQuotation.dto.request;

import java.math.BigDecimal;

import com.example.shopping.requestForQuotation.enums.RfqScoringCriterion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RfqScoreCriterionRequest(
        @NotNull
        RfqScoringCriterion criterion,
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal weightPercent
) {}
