package com.example.shopping.workflow.camunda.service;

import com.example.shopping.purchaseorder.entity.PurchaseOrder;

public interface PurchaseOrderWorkflowService {

    String startReviewProcess(PurchaseOrder order);

    void completeReview(PurchaseOrder order, String reviewer, boolean approved, String comment);
}
