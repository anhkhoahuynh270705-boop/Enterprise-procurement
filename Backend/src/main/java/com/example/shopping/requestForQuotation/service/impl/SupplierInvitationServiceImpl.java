package com.example.shopping.requestForQuotation.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.requestForQuotation.dto.request.supplierrequest.SupplierQuoteSubmissionRequest;
import com.example.shopping.requestForQuotation.dto.response.InvitationIssuedResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;
import com.example.shopping.requestForQuotation.dto.response.supplierresponse.SupplierInvitationViewResponse;
import com.example.shopping.requestForQuotation.entity.Rfq;
import com.example.shopping.requestForQuotation.entity.RfqInvitationToken;
import com.example.shopping.requestForQuotation.entity.RfqInvitedSupplier;
import com.example.shopping.requestForQuotation.enums.RfqInviteStatus;
import com.example.shopping.requestForQuotation.enums.RfqStatus;
import com.example.shopping.requestForQuotation.mapper.SupplierInvitationMapper;
import com.example.shopping.requestForQuotation.repository.RfqInvitedSupplierRepository;
import com.example.shopping.requestForQuotation.repository.RfqRepository;
import com.example.shopping.requestForQuotation.service.RfqInvitationTokenService;
import com.example.shopping.requestForQuotation.service.SupplierInvitationEmailService;
import com.example.shopping.requestForQuotation.service.SupplierInvitationService;
import com.example.shopping.requestForQuotation.service.SupplierQuoteSubmissionService;
import com.example.shopping.supplier.enums.SupplierStatus;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults (level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SupplierInvitationServiceImpl implements SupplierInvitationService {

    RfqRepository rfqRepository;
    RfqInvitedSupplierRepository invitedSupplierRepository;
    RfqInvitationTokenService tokenService;
    SupplierInvitationEmailService emailService;
    SupplierQuoteSubmissionService quoteSubmissionService;
    SupplierInvitationMapper mapper;

    @Value("${app.supplier-portal-url:http://localhost:4200/supplier/rfq/respond}")
    @NonFinal
    private String supplierPortalUrl;

    @Value("${app.rfq-invitation-ttl-hours:72}")
    @NonFinal
    private long invitationTtlHours;

    @Override
    @Transactional
    public InvitationIssuedResponse issueLink(
            UUID rfqId,
            UUID supplierId,
            String currentUsername,
            boolean isAdmin
    ) {
        Rfq rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> ErrorEnum.RFQ_NOT_FOUND.ex());

        if (!isAdmin && !Objects.equals(rfq.getCreatedBy(), currentUsername)) {
            throw ErrorEnum.RFQ_INVITATION_FORBIDDEN.ex();
        }

        validateRfQIsOpen(rfq);

        RfqInvitedSupplier invite = invitedSupplierRepository
                .findByRfq_IdAndSupplier_Id(rfqId, supplierId)
                .orElseThrow(() -> ErrorEnum.RFQ_INVITATION_NOT_FOUND.ex());

        return issueLink(rfq, invite, currentUsername);
    }

    @Override
    @Transactional
    public void sendInvitationsForApprovedRfq(UUID rfqId, String senderUsername) {
        Rfq rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> ErrorEnum.RFQ_NOT_FOUND.ex());
        if (rfq.getStatus() != RfqStatus.SENT) {
            log.warn("Bỏ qua gửi email tự động cho RFQ vì trạng thái hiện tại là",
                    rfq.getRfqCode(), rfq.getStatus());
            return;
        }
        validateRfQIsOpen(rfq);

        for (RfqInvitedSupplier invite : rfq.getInvitedSuppliers()) {
            if (invite.getStatus() == RfqInviteStatus.REVOKED) continue;
            issueLink(rfq, invite, senderUsername);
        }
    }

    private InvitationIssuedResponse issueLink(Rfq rfq, RfqInvitedSupplier invite, String senderUsername) {
        if (invite.getStatus() == RfqInviteStatus.REVOKED) {
            throw ErrorEnum.RFQ_INVITATION_REVOKED.ex();
        }
        if (invite.getSupplier().getStatus() != SupplierStatus.ACTIVE) {
            throw ErrorEnum.RFQ_SUPPLIER_NOT_ACTIVE.ex();
        }
        if (invite.getSupplier().getEmail() == null
                || invite.getSupplier().getEmail().isBlank()) {
            throw ErrorEnum.RFQ_SUPPLIER_EMAIL_MISSING.ex();
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(invitationTtlHours);
        if (expiresAt.isAfter(rfq.getDeadline())) {
            expiresAt = rfq.getDeadline();
        }

        String rawToken = tokenService.issue(invite, expiresAt, senderUsername);
        String invitationLink = supplierPortalUrl + "#token=" + rawToken;
        emailService.sendInvitation(
                invite.getSupplier().getEmail(),
                invite.getSupplier().getName(),
                rfq.getRfqCode(),
                invitationLink,
                expiresAt
        );

        return new InvitationIssuedResponse(rfq.getRfqCode(), invite.getSupplier().getName(), expiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierInvitationViewResponse resolve(String rawToken) {
        RfqInvitationToken token = tokenService.findValidForRead(rawToken);
        tokenService.validateInvitationCanReceiveQuote(token);
        return mapper.toPublicView(token.getInvitedSupplier().getRfq());
    }

    @Override
    public RfqQuoteRoundResponse submit(SupplierQuoteSubmissionRequest request) {
        return quoteSubmissionService.submit(request);
    }

    private void validateRfQIsOpen(Rfq rfq) {
        if (rfq.getStatus() != RfqStatus.SENT && rfq.getStatus() != RfqStatus.RECEIVING_QUOTES) {
            throw ErrorEnum.RFQ_NOT_OPEN.ex();
        }
        if (rfq.getDeadline() == null || !rfq.getDeadline().isAfter(LocalDateTime.now())) {
            throw ErrorEnum.RFQ_DEADLINE_PASSED.ex();
        }
    }
}
