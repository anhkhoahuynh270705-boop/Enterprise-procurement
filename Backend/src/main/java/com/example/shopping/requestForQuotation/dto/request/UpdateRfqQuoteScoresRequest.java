package com.example.shopping.requestForQuotation.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.example.shopping.requestForQuotation.enums.RfqScoringCriterion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRfqQuoteScoresRequest(
        @NotEmpty
        List<@NotNull @Valid ScoreRequest> scores
) {}
