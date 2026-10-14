package com.example.shopping.workflow.camunda.listener.supplier;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component("supplierProposalReviewTaskListener")
@RequiredArgsConstructor
public class SupplierProposalReviewTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask task) {
        boolean approved = parseDecision(task.getVariable("approved"));
        String comment = readComment(task.getVariable("reviewComment"));
        if (!approved && comment.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối đề xuất nhà cung cấp.");
        }
        if (comment.length() > 2000) {
            throw new IllegalArgumentException("Nhận xét tối đa 2000 ký tự.");
        }

        Object reviewerVariable = task.getVariable("checkerUsername");
        String reviewer = reviewerVariable == null ? "" : reviewerVariable.toString().trim();
        if (reviewer.isBlank()) {
            reviewer = task.getAssignee();
        }
        if (reviewer == null || reviewer.isBlank()) {
            throw new IllegalArgumentException(
                    "Hãy nhận nhiệm vụ trước khi hoàn tất để lưu người duyệt.");
        }

        task.setVariable("approved", approved);
        task.setVariable("checkerUsername", reviewer);
        task.setVariable("reviewComment", comment);
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
        throw new IllegalArgumentException("Vui lòng chọn kết quả duyệt hợp lệ.");
    }

    private String readComment(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
