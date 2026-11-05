package com.example.shopping.requestForQuotation.mapper;

import com.example.shopping.requestForQuotation.dto.response.supplierresponse.Item;
import com.example.shopping.requestForQuotation.dto.response.supplierresponse.SupplierInvitationViewResponse;
import com.example.shopping.requestForQuotation.entity.Rfq;
import com.example.shopping.requestForQuotation.entity.RfqItem;
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

}
