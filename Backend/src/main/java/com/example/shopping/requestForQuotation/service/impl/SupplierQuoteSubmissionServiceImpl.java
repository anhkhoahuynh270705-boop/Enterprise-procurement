package com.example.shopping.requestForQuotation.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.requestForQuotation.dto.request.QuoteItemRequest;
import com.example.shopping.requestForQuotation.dto.request.SubmitRfqQuoteRequest;
import com.example.shopping.requestForQuotation.dto.request.supplierrequest.SupplierQuoteSubmissionRequest;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;
import com.example.shopping.requestForQuotation.entity.RfqInvitationToken;
import com.example.shopping.requestForQuotation.entity.RfqInvitedSupplier;
import com.example.shopping.requestForQuotation.entity.RfqItem;
import com.example.shopping.requestForQuotation.entity.RfqQuoteItem;
import com.example.shopping.requestForQuotation.entity.RfqQuoteRound;
import com.example.shopping.requestForQuotation.enums.RfqInviteStatus;
import com.example.shopping.requestForQuotation.enums.RfqQuoteStatus;
import com.example.shopping.requestForQuotation.mapper.SupplierInvitationMapper;
import com.example.shopping.requestForQuotation.repository.RfqInvitedSupplierRepository;
import com.example.shopping.requestForQuotation.repository.RfqItemRepository;
import com.example.shopping.requestForQuotation.repository.RfqQuoteRoundRepository;
import com.example.shopping.requestForQuotation.service.RfqInvitationTokenService;
import com.example.shopping.requestForQuotation.service.SupplierQuoteSubmissionService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SupplierQuoteSubmissionServiceImpl implements SupplierQuoteSubmissionService {

    RfqInvitationTokenService tokenService;
    RfqInvitedSupplierRepository invitedSupplierRepository;
    RfqItemRepository rfqItemRepository;
    RfqQuoteRoundRepository quoteRoundRepository;
    SupplierInvitationMapper mapper;

    @Override
    @Transactional
    public RfqQuoteRoundResponse submit(SupplierQuoteSubmissionRequest request) {
        RfqInvitationToken token = tokenService.findValidForSubmission(request.token());
        tokenService.validateInvitationCanReceiveQuote(token);

        RfqInvitedSupplier invite = invitedSupplierRepository
                .findByIdForUpdate(token.getInvitedSupplier().getId())
                .orElseThrow(() -> ErrorEnum.RFQ_INVITATION_NOT_FOUND.ex());
        tokenService.validateInvitationCanReceiveQuote(token);

        SubmitRfqQuoteRequest quote = request.quote();
        if (quote.validUntil().isBefore(LocalDate.now())) {
            throw ErrorEnum.RFQ_QUOTE_VALID_UNTIL_INVALID.ex();
        }

        List<RfqItem> rfqItems = rfqItemRepository.findByRfq_IdAndActiveTrue(invite.getRfq().getId());
        Map<UUID, RfqItem> itemById = new HashMap<>();
        rfqItems.forEach(item -> itemById.put(item.getId(), item));

        Set<UUID> submittedIds = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        List<RfqQuoteItem> quoteItems = new ArrayList<>();

        for (QuoteItemRequest line : quote.items()) {
            if (!submittedIds.add(line.rfqItemId())) {
                throw ErrorEnum.RFQ_QUOTE_ITEM_DUPLICATE.ex();
            }

            RfqItem rfqItem = itemById.get(line.rfqItemId());
            if (rfqItem == null) {
                throw ErrorEnum.RFQ_QUOTE_ITEM_NOT_IN_RFQ.ex();
            }

            boolean quoted = Boolean.TRUE.equals(line.quoted());
            if (quoted && line.unitPrice() == null) {
                throw ErrorEnum.RFQ_QUOTE_ITEM_PRICE_REQUIRED.ex();
            }
            if (!quoted && line.unitPrice() != null) {
                throw ErrorEnum.RFQ_QUOTE_ITEM_PRICE_FORBIDDEN.ex();
            }

            if (quoted) {
                subtotal = subtotal.add(
                        line.unitPrice().multiply(BigDecimal.valueOf(rfqItem.getQuantity()))
                );
            }

            quoteItems.add(RfqQuoteItem.builder()
                    .rfqItem(rfqItem)
                    .quoted(quoted)
                    .unitPrice(line.unitPrice())
                    .note(line.note())
                    .build());
        }

        if (submittedIds.size() != rfqItems.size()) {
            throw ErrorEnum.RFQ_QUOTE_INCOMPLETE.ex();
        }

        RfqQuoteRound previous = quoteRoundRepository
                .findTopByRfq_IdAndSupplier_IdOrderByRoundNumberDesc(
                        invite.getRfq().getId(), invite.getSupplier().getId()
                ).orElse(null);

        int roundNumber = previous == null ? 1 : previous.getRoundNumber() + 1;
        BigDecimal total = subtotal
                .add(quote.taxAmount())
                .add(quote.shippingCost())
                .add(quote.otherCost());

        LocalDateTime now = LocalDateTime.now();
        RfqQuoteRound round = RfqQuoteRound.builder()
                .rfq(invite.getRfq())
                .supplier(invite.getSupplier())
                .roundNumber(roundNumber)
                .status(RfqQuoteStatus.SUBMITTED)
                .submittedAt(now)
                .createdAt(now)
                .validUntil(quote.validUntil())
                .deliveryDays(quote.deliveryDays())
                .paymentTerms(quote.paymentTerms())
                .warranty(quote.warranty())
                .technicalStatus(quote.technicalStatus())
                .subtotalBeforeTax(subtotal)
                .taxAmount(quote.taxAmount())
                .shippingCost(quote.shippingCost())
                .otherCost(quote.otherCost())
                .totalAfterTax(total)
                .items(quoteItems)
                .build();

        quoteItems.forEach(item -> item.setQuoteRound(round));
        RfqQuoteRound saved = quoteRoundRepository.save(round);

        invite.setStatus(RfqInviteStatus.RESPONDED);
        invite.setRespondedAt(now);
        tokenService.consume(token, now);

        return mapper.toQuoteResponse(saved);
    }

}
