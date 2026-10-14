package com.example.shopping.supplier.proposal.service.impl;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.supplier.dto.request.CreateSupplierRequestDto;
import com.example.shopping.supplier.proposal.entity.SupplierProposal;
import com.example.shopping.supplier.proposal.enums.SupplierProposalStatus;
import com.example.shopping.supplier.proposal.mapper.SupplierProposalMapper;
import com.example.shopping.supplier.proposal.repository.SupplierProposalRepository;
import com.example.shopping.supplier.repository.SupplierRepository;
import com.example.shopping.supplier.service.SupplierService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierProposalReviewCompletionService {

    SupplierProposalRepository proposals;
    SupplierRepository suppliers;
    SupplierService supplierService;
    SupplierProposalMapper proposalMapper;

    @Transactional
    public void completeReview(UUID proposalId, String reviewer, boolean approved, String comment) {
        String normalizedComment = comment == null ? "" : comment.trim();
        if (!approved && normalizedComment.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối đề xuất nhà cung cấp.");
        }
        if (normalizedComment.length() > 2000) {
            throw new IllegalArgumentException("Nhận xét tối đa 2000 ký tự.");
        }

        SupplierProposal proposal = proposals.findForReview(proposalId)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_PROPOSAL_NOT_FOUND.ex());
        if (proposal.getStatus() != SupplierProposalStatus.PENDING) {
            throw ErrorEnum.SUPPLIER_PROPOSAL_PROCESSED.ex();
        }

        if (approved) {
            if (suppliers.existsByTaxCode(proposal.getTaxCode())) {
                throw ErrorEnum.DATA_CONFLICT.ex(
                        "Mã số thuế đã tồn tại trong danh sách nhà cung cấp.");
            }

            CreateSupplierRequestDto supplierRequest = proposalMapper.toCreateSupplierRequest(proposal);
            try {
                var supplier = supplierService.createSupplier(supplierRequest);
                suppliers.flush();
                proposal.setSupplierId(supplier.getId());
            } catch (DataIntegrityViolationException exception) {
                throw ErrorEnum.DATA_CONFLICT.ex(
                        "Nhà cung cấp bị trùng với dữ liệu vừa được cập nhật. Vui lòng tải lại và thử lại.",
                        exception);
            }
            proposal.setStatus(SupplierProposalStatus.APPROVED);
        } else {
            proposal.setStatus(SupplierProposalStatus.REJECTED);
        }

        proposal.setPendingTaxCode(null);
        proposal.setReviewedBy(reviewer);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewComment(normalizedComment.isBlank() ? null : normalizedComment);
        proposals.saveAndFlush(proposal);
    }
}
