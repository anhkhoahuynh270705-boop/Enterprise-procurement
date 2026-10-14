package com.example.shopping.workflow.camunda.delegate.supplierDelegate;

import java.util.UUID;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import com.example.shopping.supplier.proposal.service.impl.SupplierProposalReviewCompletionService;

import lombok.RequiredArgsConstructor;

@Component("approveSupplierProposalDelegate")
@RequiredArgsConstructor
public class ApproveSupplierProposalDelegate implements JavaDelegate {

    private final SupplierProposalReviewCompletionService reviewCompletionService;

    @Override
    public void execute(DelegateExecution execution) {
        reviewCompletionService.completeReview(
                proposalId(execution),
                reviewer(execution),
                true,
                comment(execution));
    }

    private UUID proposalId(DelegateExecution execution) {
        Object value = execution.getVariable("proposalId");
        if (value == null) {
            throw new IllegalArgumentException("Thiếu proposalId trong Camunda process variables.");
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("proposalId trong Camunda không hợp lệ.", exception);
        }
    }

    private String reviewer(DelegateExecution execution) {
        Object value = execution.getVariable("checkerUsername");
        if (value == null || value.toString().isBlank()) {
            throw new IllegalArgumentException("Không xác định được người duyệt đề xuất nhà cung cấp.");
        }
        return value.toString().trim();
    }

    private String comment(DelegateExecution execution) {
        Object value = execution.getVariable("reviewComment");
        return value == null ? "" : value.toString();
    }
}
