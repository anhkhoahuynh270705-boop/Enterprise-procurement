package com.example.shopping.requestForQuotation.dto.response;

import java.time.LocalDateTime;

public record InvitationIssuedResponse(
        String rfqCode,
        String supplierName,
        LocalDateTime expiresAt
) {}
