package com.example.shopping.requestForQuotation.service;

import com.example.shopping.requestForQuotation.entity.RfqInvitationToken;
import com.example.shopping.requestForQuotation.entity.RfqInvitedSupplier;

import java.time.LocalDateTime;

public interface RfqInvitationTokenService {

    String issue(RfqInvitedSupplier invite, LocalDateTime expiresAt, String createdBy);

    RfqInvitationToken findValidForRead(String rawToken);

    RfqInvitationToken findValidForSubmission(String rawToken);

    void validateInvitationCanReceiveQuote(RfqInvitationToken token);

    void consume(RfqInvitationToken token, LocalDateTime consumedAt);
}
