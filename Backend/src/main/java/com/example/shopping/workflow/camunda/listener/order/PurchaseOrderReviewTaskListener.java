package com.example.shopping.workflow.camunda.listener.order;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

import com.example.shopping.purchaseorder.service.impl.PurchaseOrderReviewServiceImpl;

import lombok.RequiredArgsConstructor;

@Component("purchaseOrderReviewTaskListener")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PurchaseOrderReviewTaskListener implements TaskListener {

    PurchaseOrderReviewServiceImpl purchaseOrderReviewServiceImpl;

    @Override
    public void notify(DelegateTask task) {
        UUID orderId = parseOrderId(task.getVariable("purchaseOrderId"));
        boolean approved = parseDecision(task.getVariable("approved"));
        String comment = readComment(task.getVariable("reviewComment"));
        purchaseOrderReviewServiceImpl.completeReview(orderId, approved, resolveReviewer(task), comment);
    }

    private UUID parseOrderId(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Thiếu purchaseOrder Id");
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("purchaseOrderId không hợp lệ.", exception);
        }
    }

    private boolean parseDecision(Object value) {
        if (value instanceof Boolean approved) {
            return approved;
        }
        if (value instanceof String approved) {
            if ("true".equalsIgnoreCase(approved)) {
                return true;
            }
            if ("false".equalsIgnoreCase(approved)) {
                return false;
            }
        }
        throw new IllegalArgumentException("Thiếu kết quả duyệt hợp lệ process variables.");
    }

    private String readComment(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private String resolveReviewer(DelegateTask task) {
        Object reviewerVariable = task.getVariable("reviewer");
        if (reviewerVariable != null && !reviewerVariable.toString().isBlank()) {
            return reviewerVariable.toString().trim();
        }
        if (task.getAssignee() != null && !task.getAssignee().isBlank()) {
            return task.getAssignee();
        }
        return null;
    }
}
