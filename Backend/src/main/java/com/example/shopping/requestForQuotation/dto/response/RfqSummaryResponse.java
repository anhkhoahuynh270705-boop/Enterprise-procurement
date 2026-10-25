package com.example.shopping.requestForQuotation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.shopping.requestForQuotation.enums.RfqStatus;

public record RfqSummaryResponse(
        UUID id,
        String rfqCode,
        String title,
        UUID procurementTicketId,
        String procurementTicketCode,
        String department,
        int respondedSupplierCount,
        int invitedSupplierCount,
        LocalDateTime deadline,
        BigDecimal estimatedValue,
        RfqStatus status,
        String createdBy,
        String creatorName,
        LocalDateTime createdAt) {
}
