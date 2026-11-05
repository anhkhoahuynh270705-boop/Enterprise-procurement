package com.example.shopping.supplier.proposal.service;

import com.example.shopping.supplier.proposal.dto.request.SupplierProposalRequest;
import com.example.shopping.supplier.proposal.dto.request.SupplierProposalReviewRequest;
import com.example.shopping.supplier.proposal.dto.request.SupplierProposalSearchCriteria;
import com.example.shopping.supplier.proposal.dto.response.SupplierProposalResponseDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierProposalService {
    List<SupplierProposalResponseDto> list();
    Page<SupplierProposalResponseDto> search(SupplierProposalSearchCriteria criteria, Pageable pageable);
    long pendingCount();
    SupplierProposalResponseDto submit(SupplierProposalRequest request);
    SupplierProposalResponseDto review(UUID id, SupplierProposalReviewRequest request);
}
