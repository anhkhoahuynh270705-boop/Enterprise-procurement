package com.example.shopping.requestForQuotation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.shopping.requestForQuotation.enums.RfqQuoteStatus;
import com.example.shopping.requestForQuotation.enums.RfqTechnicalStatus;

public record RfqQuoteRoundResponse(
        UUID id,
        UUID supplierId,
        String supplierCode,
        String supplierName,
        int roundNumber,
        RfqQuoteStatus status,
        LocalDateTime submittedAt,
        LocalDate validUntil,
        Integer deliveryDays,
        String paymentTerms,
        String warranty,
        RfqTechnicalStatus technicalStatus,
        BigDecimal subtotalBeforeTax,
        BigDecimal taxAmount,
        BigDecimal shippingCost,
        BigDecimal otherCost,
        BigDecimal totalAfterTax,
        BigDecimal totalScore,
        Integer rank,
        boolean incomplete,
        List<QuoteItemResponse> items,
        List<QuoteScoreResponse> scores) {
}
