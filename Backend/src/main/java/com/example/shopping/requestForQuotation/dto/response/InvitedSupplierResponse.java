package com.example.shopping.requestForQuotation.dto.response;

import com.example.shopping.requestForQuotation.enums.RfqInviteStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvitedSupplierResponse(
        UUID supplierId,
        String supplierCode,
        String supplierName,
        RfqInviteStatus status,
        LocalDateTime invitedAt,
        LocalDateTime respondedAt) {
}
