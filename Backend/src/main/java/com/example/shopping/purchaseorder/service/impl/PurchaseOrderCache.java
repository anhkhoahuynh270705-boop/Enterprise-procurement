package com.example.shopping.purchaseorder.service.impl;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.purchaseorder.dto.response.PurchaseOrderResponse;
import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;
import com.example.shopping.purchaseorder.mapper.PurchaseOrderMapper;
import com.example.shopping.purchaseorder.repository.PurchaseOrderRepository;
import com.example.shopping.security.TicketAccess;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PurchaseOrderCache {

    static final String CACHE_NAME = "purchase_orders";

    PurchaseOrderRepository purchaseOrderRepository;
    PurchaseOrderMapper purchaseOrderMapper;

    @Cacheable(cacheNames = CACHE_NAME,
            key = "T(com.example.shopping.security.TicketAccess).cacheScope() + ':' + (#status == null ? 'ALL' : #status.name())")
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getPurchaseOrders(PurchaseOrderStatus status) {
        boolean isStaffOrChecker = TicketAccess.hasRole("ADMIN") || TicketAccess.hasRole("CHECKER");
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(order -> isStaffOrChecker
                        || order.getStatus() == PurchaseOrderStatus.SENT
                        || order.getStatus() == PurchaseOrderStatus.RECEIVED)
                .filter(order -> status == null || order.getStatus() == status)
                .map(purchaseOrderMapper::toResponse)
                .toList();
    }
}
