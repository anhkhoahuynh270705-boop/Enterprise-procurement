package com.example.shopping.workflow.camunda.service;

import com.example.shopping.supplier.proposal.entity.SupplierProposal;

public interface SupplierProposalWorkflowService {
    String startReviewProcess(SupplierProposal proposal);

    void completeCheckerReview(
            SupplierProposal proposal, String checkerUsername, boolean approved, String comment);
}
