package com.example.shopping.purchaseorder.service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.shopping.purchaseorder.dto.request.CreatePurchaseOrderRequest;
import com.example.shopping.purchaseorder.dto.request.PurchaseOrderSearchCriteria;
import com.example.shopping.purchaseorder.dto.request.ReviewPurchaseOrderRequest;
import com.example.shopping.purchaseorder.dto.response.PurchaseOrderResponse;
import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;

public interface PurchaseOrderService {

    List<PurchaseOrderResponse> getPurchaseOrders(PurchaseOrderStatus status);

    Page<PurchaseOrderResponse> searchPurchaseOrders(PurchaseOrderSearchCriteria criteria, Pageable pageable);

    List<UUID> getOrderedProcurementItemIds(UUID ticketId);

    PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request);

    PurchaseOrderResponse updateStatus(UUID id, PurchaseOrderStatus status);

    PurchaseOrderResponse review(UUID id, ReviewPurchaseOrderRequest request);
}
