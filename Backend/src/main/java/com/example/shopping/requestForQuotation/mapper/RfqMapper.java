package com.example.shopping.requestForQuotation.mapper;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.shopping.requestForQuotation.dto.response.InvitedSupplierResponse;
import com.example.shopping.requestForQuotation.dto.response.ItemAwardResponse;
import com.example.shopping.requestForQuotation.dto.response.ItemResponse;
import com.example.shopping.requestForQuotation.dto.response.QuoteItemResponse;
import com.example.shopping.requestForQuotation.dto.response.QuoteScoreResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqAuditLogResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqDetailResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqSummaryResponse;
import com.example.shopping.requestForQuotation.dto.response.ScoreCriterionResponse;
import com.example.shopping.requestForQuotation.entity.Rfq;
import com.example.shopping.requestForQuotation.entity.RfqAuditLog;
import com.example.shopping.requestForQuotation.entity.RfqInvitedSupplier;
import com.example.shopping.requestForQuotation.entity.RfqItem;
import com.example.shopping.requestForQuotation.entity.RfqItemAward;
import com.example.shopping.requestForQuotation.entity.RfqQuoteItem;
import com.example.shopping.requestForQuotation.entity.RfqQuoteRound;
import com.example.shopping.requestForQuotation.entity.RfqQuoteScore;
import com.example.shopping.requestForQuotation.entity.RfqScoreCriterion;
import com.example.shopping.requestForQuotation.enums.RfqInviteStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RfqMapper {

    /* RFQ -> RfqSummaryResponse */
    @Mapping(target = "procurementTicketId", source = "rfq.procurementTicket.id")
    @Mapping(target = "procurementTicketCode", source = "rfq.procurementTicket.ticketCode")
    @Mapping(target = "respondedSupplierCount", expression = "java(respondedSupplierCount(rfq))")
    @Mapping(target = "invitedSupplierCount", expression = "java(rfq.getInvitedSuppliers().size())")
    RfqSummaryResponse toSummary(Rfq rfq, String creatorName);

    /* RFQ -> RfqDetailResponse */
    @Mapping(target = "procurementTicketId", source = "rfq.procurementTicket.id")
    @Mapping(target = "procurementTicketCode", source = "rfq.procurementTicket.ticketCode")
    @Mapping(target = "procurementTicketTitle", source = "rfq.procurementTicket.title")
    @Mapping(target = "selectedSupplierId", source = "rfq.selectedSupplier.id")
    @Mapping(target = "selectedSupplierName", source = "rfq.selectedSupplier.name")
    @Mapping(target = "history", expression = "java(sortedHistory(rfq))")
    RfqDetailResponse toDetail(Rfq rfq, String creatorName, List<RfqQuoteRoundResponse> quoteRounds);

    /* RFQ Item -> ItemResponse */
    @Mapping(target = "procurementItemId", source = "procurementItem.id")
    ItemResponse toItemResponse(RfqItem entity);

    /* RFQ Invited Supplier -> InvitedSupplierResponse */
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierCode", source = "supplier.code")
    @Mapping(target = "supplierName", source = "supplier.name")
    InvitedSupplierResponse toInvitedSupplierResponse(RfqInvitedSupplier entity);

    /* RFQ Score Criterion -> ScoreCriterionResponse */
    ScoreCriterionResponse toScoreCriterionResponse(RfqScoreCriterion entity);

    /* RFQ Item Award -> ItemAwardResponse */
    @Mapping(target = "rfqItemId", source = "rfqItem.id")
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierName", source = "supplier.name")
    ItemAwardResponse toItemAwardResponse(RfqItemAward entity);

    /* RFQ Audit Log -> RfqAuditLogResponse */
    RfqAuditLogResponse toAuditLogResponse(RfqAuditLog entity);

    /* RFQ Quote Round -> RfqQuoteRoundResponse */
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierCode", source = "supplier.code")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "totalScore", ignore = true)
    @Mapping(target = "rank", ignore = true)
    @Mapping(target = "incomplete", expression = "java(entity.getItems().stream().anyMatch(i -> !Boolean.TRUE.equals(i.getQuoted())))")
    RfqQuoteRoundResponse toQuoteResponse(RfqQuoteRound entity);

    /* RFQ Quote Item -> QuoteItemResponse */
    @Mapping(target = "rfqItemId", source = "rfqItem.id")
    QuoteItemResponse toQuoteItemResponse(RfqQuoteItem entity);

    /* RFQ Quote Score -> QuoteScoreResponse */
    QuoteScoreResponse toQuoteScoreResponse(RfqQuoteScore entity);

    default RfqQuoteRoundResponse withScoreAndRank(
            RfqQuoteRoundResponse response,
            BigDecimal totalScore,
            Integer rank) {
        return new RfqQuoteRoundResponse(response.id(), response.supplierId(), response.supplierCode(),
                response.supplierName(), response.roundNumber(), response.status(), response.submittedAt(),
                response.validUntil(), response.deliveryDays(), response.paymentTerms(), response.warranty(),
                response.technicalStatus(), response.subtotalBeforeTax(), response.taxAmount(),
                response.shippingCost(), response.otherCost(), response.totalAfterTax(), totalScore,
                rank, response.incomplete(), response.items(), response.scores());
    }

    default int respondedSupplierCount(Rfq rfq) {
        return (int) rfq.getInvitedSuppliers().stream()
                .filter(invite -> invite.getStatus() == RfqInviteStatus.RESPONDED)
                .count();
    }

    default List<RfqAuditLogResponse> sortedHistory(Rfq rfq) {
        return rfq.getAuditLogs().stream()
                .sorted(Comparator.comparing(
                        RfqAuditLog::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toAuditLogResponse)
                .toList();
    }
}
