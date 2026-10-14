package com.example.shopping.purchaseorder.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.purchaseorder.entity.PurchaseOrder;
import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;
import com.example.shopping.purchaseorder.repository.PurchaseOrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurchaseOrderReviewServiceImpl {

    private final PurchaseOrderRepository purchaseOrderRepository;

    @Transactional
    public void completeReview(UUID orderId, boolean approved, String reviewer, String comment) {
        String normalizedComment = comment == null ? "" : comment.trim();
        if (!approved && normalizedComment.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối đơn đặt hàng.");
        }
        if (normalizedComment.length() > 2000) {
            throw new IllegalArgumentException("Nhận xét tối đa 2000 ký tự.");
        }

        PurchaseOrder order = purchaseOrderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> ErrorEnum.PURCHASE_ORDER_NOT_FOUND.ex());
        if (order.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL
                && order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw ErrorEnum.PURCHASE_ORDER_NOT_PENDING.ex();
        }

        LocalDateTime reviewedAt = LocalDateTime.now();
        order.setStatus(approved ? PurchaseOrderStatus.SENT : PurchaseOrderStatus.REJECTED);
        order.setReviewedBy(reviewer);
        order.setReviewedAt(reviewedAt);
        order.setReviewComment(normalizedComment.isBlank() ? null : normalizedComment);
        order.setUpdatedAt(reviewedAt);
        purchaseOrderRepository.save(order);
    }
}
