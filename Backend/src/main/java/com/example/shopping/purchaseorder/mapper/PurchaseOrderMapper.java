package com.example.shopping.purchaseorder.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.shopping.purchaseorder.dto.response.PurchaseOrderItemResponse;
import com.example.shopping.purchaseorder.dto.response.PurchaseOrderResponse;
import com.example.shopping.purchaseorder.entity.PurchaseOrder;
import com.example.shopping.purchaseorder.entity.PurchaseOrderItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PurchaseOrderMapper {

    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "ticket.ticketCode", target = "ticketCode")
    @Mapping(source = "ticket.title", target = "ticketTitle")
    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.code", target = "supplierCode")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "ticket.currency", target = "currency")
    PurchaseOrderResponse toResponse(PurchaseOrder order);

    @Mapping(source = "procurementItem.id", target = "procurementItemId")
    PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item);
}
