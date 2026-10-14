package com.example.shopping.purchaseorder.service;

import java.util.List;
import java.util.UUID;

import com.example.shopping.purchaseorder.dto.request.CreatePurchaseOrderRequest;
import com.example.shopping.purchaseorder.dto.request.ReviewPurchaseOrderRequest;
import com.example.shopping.purchaseorder.dto.response.PurchaseOrderResponse;
import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;

public interface PurchaseOrderService {

    List<PurchaseOrderResponse> getPurchaseOrders(PurchaseOrderStatus status);

    PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request);

    PurchaseOrderResponse updateStatus(UUID id, PurchaseOrderStatus status);

    PurchaseOrderResponse review(UUID id, ReviewPurchaseOrderRequest request);
}
