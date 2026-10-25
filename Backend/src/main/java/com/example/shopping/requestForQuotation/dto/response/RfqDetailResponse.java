package com.example.shopping.requestForQuotation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.shopping.requestForQuotation.enums.RfqStatus;

public record RfqDetailResponse(
        UUID id,
        String rfqCode,
        String title,
        UUID procurementTicketId,
        String procurementTicketCode,
        String procurementTicketTitle,
        String department,
        RfqStatus status,
        LocalDateTime deadline,
        int quoteValidityDays,
        String deliveryLocation,
        String deliveryWindow,
        String currency,
        BigDecimal estimatedValue,
        String createdBy,
        String creatorName,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewReason,
        UUID selectedSupplierId,
        String selectedSupplierName,
        String selectionReason,
        String exceptionReason,
        List<ItemResponse> items,
        List<InvitedSupplierResponse> invitedSuppliers,
        List<ScoreCriterionResponse> scoreCriteria,
        List<RfqQuoteRoundResponse> quoteRounds,
        List<ItemAwardResponse> itemAwards,
        List<RfqAuditLogResponse> history) {
}
