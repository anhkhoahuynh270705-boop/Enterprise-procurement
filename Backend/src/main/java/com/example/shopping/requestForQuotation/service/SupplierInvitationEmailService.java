package com.example.shopping.requestForQuotation.service;

import java.time.LocalDateTime;

public interface SupplierInvitationEmailService {
    void sendInvitation(String recipientEmail, String supplierName, String rfqCode,
                        String invitationLink, LocalDateTime expiresAt);
}
