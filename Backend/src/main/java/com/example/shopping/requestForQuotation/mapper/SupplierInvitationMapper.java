package com.example.shopping.requestForQuotation.mapper;

import com.example.shopping.requestForQuotation.dto.response.QuoteItemResponse;
import com.example.shopping.requestForQuotation.dto.response.QuoteScoreResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;
import com.example.shopping.requestForQuotation.dto.response.supplierresponse.Item;
import com.example.shopping.requestForQuotation.dto.response.supplierresponse.SupplierInvitationViewResponse;
import com.example.shopping.requestForQuotation.entity.*;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SupplierInvitationMapper {
    /* RFQ */
    @Mapping(target = "rfqCode", source = "rfqCode")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "deadline", source = "deadline")
    @Mapping(target = "deliveryLocation", source = "deliveryLocation")
    @Mapping(target = "deliveryWindow", source = "deliveryWindow")
    @Mapping(target = "currency", source = "currency")
    @Mapping(
            target = "items",
            expression = "java(rfq.getItems().stream()"
                    + ".filter(item -> Boolean.TRUE.equals(item.getActive()))"
                    + ".map(this::toPublicItem).toList())"
    )
    SupplierInvitationViewResponse toPublicView(Rfq rfq);

    /* RFQ ITEM */
    @Mapping(target = "rfqItemId", source = "id")
    Item toPublicItem(RfqItem item);

    /* RfqQuoteRound */
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "supplierCode", source = "supplier.code")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "totalScore", ignore = true)
    @Mapping(target = "rank", ignore = true)
    @Mapping(
            target = "incomplete",
            expression = "java(entity.getItems().stream().anyMatch(i -> !Boolean.TRUE.equals(i.getQuoted())))"
    )
    @Mapping(target = "items", source = "items")
    @Mapping(target = "scores", source = "scores")
    RfqQuoteRoundResponse toQuoteResponse(RfqQuoteRound entity);

    /* RfqQuoteItem */
    @Mapping(target = "rfqItemId", source = "rfqItem.id")
    QuoteItemResponse toQuoteItemResponse(RfqQuoteItem entity);

    /* RfqQuoteScore */
    QuoteScoreResponse toQuoteScoreResponse(RfqQuoteScore entity);

}
