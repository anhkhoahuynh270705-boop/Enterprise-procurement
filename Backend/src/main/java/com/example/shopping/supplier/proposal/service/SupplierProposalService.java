package com.example.shopping.supplier.proposal.service;

import com.example.shopping.supplier.proposal.dto.request.SupplierProposalRequest;
import com.example.shopping.supplier.proposal.dto.request.SupplierProposalReviewRequest;
import com.example.shopping.supplier.proposal.dto.response.SupplierProposalResponseDto;
import java.util.List;
import java.util.UUID;

public interface SupplierProposalService {
    List<SupplierProposalResponseDto> list();
    long pendingCount();
    SupplierProposalResponseDto submit(SupplierProposalRequest request);
    SupplierProposalResponseDto review(UUID id, SupplierProposalReviewRequest request);
}
