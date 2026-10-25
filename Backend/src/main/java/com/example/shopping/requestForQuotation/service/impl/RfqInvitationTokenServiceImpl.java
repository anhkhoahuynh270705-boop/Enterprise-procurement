package com.example.shopping.requestForQuotation.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.requestForQuotation.entity.RfqInvitationToken;
import com.example.shopping.requestForQuotation.entity.RfqInvitedSupplier;
import com.example.shopping.requestForQuotation.enums.RfqInviteStatus;
import com.example.shopping.requestForQuotation.enums.RfqStatus;
import com.example.shopping.requestForQuotation.repository.RfqInvitationTokenRepository;
import com.example.shopping.requestForQuotation.service.RfqInvitationTokenService;
import com.example.shopping.supplier.enums.SupplierStatus;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults (level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class RfqInvitationTokenServiceImpl implements RfqInvitationTokenService {

    RfqInvitationTokenRepository tokenRepository;
    SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public String issue(RfqInvitedSupplier invite, LocalDateTime expiresAt, String createdBy) {
        tokenRepository.revokeActiveTokens(invite.getId());

        String rawToken = generateToken();
        tokenRepository.save(RfqInvitationToken.builder()
                .invitedSupplier(invite)
                .tokenHash(sha256(rawToken))
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build());

        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public RfqInvitationToken findValidForRead(String rawToken) {
        validateRawToken(rawToken);
        RfqInvitationToken token = tokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> ErrorEnum.RFQ_INVITATION_TOKEN_INVALID.ex());
        validateTokenState(token);
        return token;
    }

    @Override
    @Transactional
    public RfqInvitationToken findValidForSubmission(String rawToken) {
        validateRawToken(rawToken);
        RfqInvitationToken token = tokenRepository.findByHashForUpdate(sha256(rawToken))
                .orElseThrow(() -> ErrorEnum.RFQ_INVITATION_TOKEN_INVALID.ex());
        validateTokenState(token);
        return token;
    }

    @Override
    public void validateInvitationCanReceiveQuote(RfqInvitationToken token) {
        RfqInvitedSupplier invite = token.getInvitedSupplier();
        if (invite.getStatus() == RfqInviteStatus.REVOKED) {
            throw ErrorEnum.RFQ_INVITATION_REVOKED.ex();
        }
        if (invite.getSupplier().getStatus() != SupplierStatus.ACTIVE) {
            throw ErrorEnum.RFQ_SUPPLIER_NOT_ACTIVE.ex();
        }

        RfqStatus status = invite.getRfq().getStatus();
        if (status != RfqStatus.SENT && status != RfqStatus.RECEIVING_QUOTES) {
            throw ErrorEnum.RFQ_NOT_OPEN.ex();
        }
        if (invite.getRfq().getDeadline() == null
                || !invite.getRfq().getDeadline().isAfter(LocalDateTime.now())) {
            throw ErrorEnum.RFQ_DEADLINE_PASSED.ex();
        }
    }

    @Override
    public void consume(RfqInvitationToken token, LocalDateTime consumedAt) {
        token.setConsumedAt(consumedAt);
    }

    private void validateTokenState(RfqInvitationToken token) {
        LocalDateTime now = LocalDateTime.now();
        if (token.getConsumedAt() != null
                || token.getRevokedAt() != null
                || token.getExpiresAt() == null
                || !token.getExpiresAt().isAfter(now)) {
            throw ErrorEnum.RFQ_INVITATION_TOKEN_INVALID.ex();
        }
    }

    private void validateRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw ErrorEnum.RFQ_INVITATION_TOKEN_MISSING.ex();
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw ErrorEnum.RFQ_TOKEN_HASH_FAILED.ex(exception);
        }
    }
}
