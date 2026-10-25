package com.example.shopping.requestForQuotation.service;

import com.example.shopping.requestForQuotation.dto.request.supplierrequest.SupplierQuoteSubmissionRequest;
import com.example.shopping.requestForQuotation.dto.response.InvitationIssuedResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;
import com.example.shopping.requestForQuotation.dto.response.supplierresponse.SupplierInvitationViewResponse;

import java.util.UUID;

public interface SupplierInvitationService {

    InvitationIssuedResponse issueLink(UUID rfqId, UUID supplierId, String currentUsername, boolean isAdmin);

    void sendInvitationsForApprovedRfq(UUID rfqId, String senderUsername);

    SupplierInvitationViewResponse resolve(String rawToken);

    RfqQuoteRoundResponse submit(SupplierQuoteSubmissionRequest request);
}
