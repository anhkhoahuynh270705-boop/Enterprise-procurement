package com.example.shopping.supplier.proposal.service.impl;

import com.example.shopping.supplier.proposal.service.SupplierProposalService;

import com.example.shopping.supplier.proposal.dto.request.SupplierProposalRequest;
import com.example.shopping.supplier.proposal.dto.request.SupplierProposalReviewRequest;
import com.example.shopping.supplier.proposal.dto.response.SupplierProposalResponseDto;
import com.example.shopping.supplier.proposal.entity.SupplierProposal;
import com.example.shopping.supplier.proposal.enums.SupplierProposalStatus;
import com.example.shopping.supplier.proposal.mapper.SupplierProposalMapper;
import com.example.shopping.supplier.proposal.repository.SupplierProposalRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.supplier.repository.SupplierRepository;
import com.example.shopping.workflow.camunda.service.SupplierProposalWorkflowService;
import lombok.RequiredArgsConstructor;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierProposalServiceImpl implements SupplierProposalService {
    SupplierProposalRepository proposals;
    SupplierRepository suppliers;
    SupplierProposalWorkflowService workflow;
    Validator validator;
    SupplierProposalMapper mapper;

    @Transactional(readOnly = true)
    public List<SupplierProposalResponseDto> list() {
        requireRole("ADMIN", "USER", "CHECKER");
        var rows = (TicketAccess.hasRole("ADMIN") || TicketAccess.hasRole("CHECKER")) ? proposals.findAllByOrderBySubmittedAtDesc()
            : proposals.findBySubmittedByOrderBySubmittedAtDesc(TicketAccess.username());
        return rows.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        requireRole("ADMIN", "CHECKER");
        return proposals.countByStatus(SupplierProposalStatus.PENDING);
    }

    @Transactional
    public SupplierProposalResponseDto submit(SupplierProposalRequest request) {
        requireRole("USER", "CHECKER");
        validate(request);
        checkSupplier(request.taxCode());
        if (proposals.existsByPendingTaxCode(request.taxCode())) {
            throw conflict("Mã số thuế này đã có đề xuất đang chờ duyệt.");
        }
        SupplierProposal proposal = new SupplierProposal();
        proposal.setName(request.name());
        proposal.setTaxCode(request.taxCode());
        proposal.setContactPerson(request.contactPerson());
        proposal.setEmail(request.email());
        proposal.setPhone(request.phone());
        proposal.setAddress(request.address());
        proposal.setCity(request.city());
        proposal.setCountry(request.country());
        proposal.setBankName(request.bankName());
        proposal.setBankAccount(request.bankAccount());
        proposal.setNotes(request.notes());
        proposal.setReason(request.reason());
        proposal.setSubmittedBy(TicketAccess.username());
        proposal.setSubmittedAt(LocalDateTime.now());
        proposal.setPendingTaxCode(request.taxCode());
        SupplierProposal saved;
        try {
            saved = proposals.saveAndFlush(proposal);
        } catch (DataIntegrityViolationException ex) {
            throw conflict("Đề xuất bị trùng với dữ liệu vừa được cập nhật. Vui lòng tải lại danh sách.");
        }
        workflow.startReviewProcess(saved);
        return mapper.toDto(proposals.saveAndFlush(saved));
    }

    @Transactional
    public SupplierProposalResponseDto review(UUID id, SupplierProposalReviewRequest request) {
        requireRole("ADMIN", "CHECKER");
        validate(request);
        SupplierProposal proposal = proposals.findForReview(id)
            .orElseThrow(() -> ErrorEnum.SUPPLIER_PROPOSAL_NOT_FOUND.ex());
        if (proposal.getStatus() != SupplierProposalStatus.PENDING) {
            throw ErrorEnum.SUPPLIER_PROPOSAL_PROCESSED.ex();
        }
        if (!TicketAccess.hasRole("ADMIN") && TicketAccess.username().equals(proposal.getSubmittedBy())) {
            throw ErrorEnum.SUPPLIER_PROPOSAL_SELF_REVIEW.ex();
        }
        if (request.approved()) {
            checkSupplier(proposal.getTaxCode());
        }
        workflow.completeCheckerReview(proposal, TicketAccess.username(), request.approved(), request.comment());
        SupplierProposal reviewed = proposals.findForReview(id)
            .orElseThrow(() -> ErrorEnum.SUPPLIER_PROPOSAL_NOT_FOUND.ex());
        return mapper.toDto(reviewed);
    }

    private void checkSupplier(String taxCode) {
        if (suppliers.existsByTaxCode(taxCode)) 
            throw conflict("Mã số thuế đã tồn tại trong danh sách nhà cung cấp.");
    }

    private <T> void validate(T request) {
        var errors = validator.validate(request);
        if (!errors.isEmpty()) 
            throw ErrorEnum.SUPPLIER_PROPOSAL_INVALID.ex(
                    errors.stream().map(e -> e.getMessage()).sorted().collect(Collectors.joining("; ")));
    }

    private void requireRole(String... roles) {
        for (String role : roles) 
            if (TicketAccess.hasRole(role)) 
                return;
        throw ErrorEnum.ACCESS_DENIED.ex();
    }

    private ResponseStatusException conflict(String message) {
        return ErrorEnum.DATA_CONFLICT.ex(message);
    }
}
