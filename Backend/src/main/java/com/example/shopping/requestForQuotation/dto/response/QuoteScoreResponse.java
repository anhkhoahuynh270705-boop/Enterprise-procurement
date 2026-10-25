package com.example.shopping.requestForQuotation.dto.response;

import com.example.shopping.requestForQuotation.enums.RfqScoringCriterion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record QuoteScoreResponse(
        RfqScoringCriterion criterion,
        BigDecimal score,
        String note,
        String updatedBy,
        LocalDateTime updatedAt) {
}
