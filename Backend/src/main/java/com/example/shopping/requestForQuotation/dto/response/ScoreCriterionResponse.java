package com.example.shopping.requestForQuotation.dto.response;

import com.example.shopping.requestForQuotation.enums.RfqScoringCriterion;
import java.math.BigDecimal;

public record ScoreCriterionResponse(
        RfqScoringCriterion criterion,
        BigDecimal weightPercent) {
}
