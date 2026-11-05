package com.example.shopping.workflow.camunda.delegate.orderDelegate;

import java.util.UUID;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.shopping.purchaseorder.service.impl.PurchaseOrderReviewServiceImpl;

import lombok.RequiredArgsConstructor;

@Component("purchaseOrderApprovalDelegate")
@RequiredArgsConstructor
public class ApprovePurchaseOrderDelegate implements JavaDelegate {

    private final PurchaseOrderReviewServiceImpl reviewService;

    @Override
    public void execute(DelegateExecution execution) {
        reviewService.completeReview(
                parseOrderId(execution.getVariable("purchaseOrderId")),
                true,
                requiredText(execution.getVariable("reviewer"), "Không xác định được người duyệt đơn đặt hàng."),
                text(execution.getVariable("reviewComment")));
    }

    private UUID parseOrderId(Object value) {
        String text = requiredText(value, "Thiếu purchaseOrderId trong Camunda process variables.");
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("purchaseOrderId trong Camunda không hợp lệ.", exception);
        }
    }

    private String requiredText(Object value, String message) {
        if (value == null || value.toString().isBlank()) throw new IllegalArgumentException(message);
        return value.toString().trim();
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
