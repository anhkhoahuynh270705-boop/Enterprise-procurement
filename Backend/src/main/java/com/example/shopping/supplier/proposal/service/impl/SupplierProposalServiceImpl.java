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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.supplier.dto.request.CreateSupplierRequestDto;
import com.example.shopping.supplier.repository.SupplierRepository;
import com.example.shopping.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierProposalServiceImpl implements SupplierProposalService {
    private final SupplierProposalRepository proposals;
    private final SupplierRepository suppliers;
    private final SupplierService supplierService;
    private final Validator validator;
    private final SupplierProposalMapper mapper;

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
        try {
            return mapper.toDto(proposals.saveAndFlush(proposal));
        } catch (DataIntegrityViolationException ex) {
            throw conflict("Đề xuất bị trùng với dữ liệu vừa được cập nhật. Vui lòng tải lại danh sách.");
        }
    }

    @Transactional
    public SupplierProposalResponseDto review(UUID id, SupplierProposalReviewRequest request) {
        requireRole("ADMIN", "CHECKER");
        validate(request);
        SupplierProposal proposal = proposals.findForReview(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đề xuất."));
        if (proposal.getStatus() != SupplierProposalStatus.PENDING) {
            throw conflict("Đề xuất này đã được xử lý. Vui lòng tải lại danh sách.");
        }
        if (!TicketAccess.hasRole("ADMIN") && TicketAccess.username().equals(proposal.getSubmittedBy())) {
            throw new AccessDeniedException("Checker không được tự duyệt đề xuất của mình.");
        }
        if (request.approved()) {
            checkSupplier(proposal.getTaxCode());
            var supplierRequest = CreateSupplierRequestDto.builder()
                .name(proposal.getName()).taxCode(proposal.getTaxCode())
                .contactPerson(proposal.getContactPerson()).email(proposal.getEmail())
                .phone(proposal.getPhone()).address(proposal.getAddress())
                .city(proposal.getCity()).country(proposal.getCountry())
                .bankName(proposal.getBankName()).bankAccount(proposal.getBankAccount())
                .notes(proposal.getNotes()).build();
            try {
                var supplier = supplierService.createSupplier(supplierRequest);
                suppliers.flush();
                proposal.setSupplierId(supplier.getId());
            } catch (DataIntegrityViolationException ex) {
                throw conflict("Nhà cung cấp bị trùng với dữ liệu vừa được cập nhật. Vui lòng tải lại và thử lại.");
            }
            proposal.setStatus(SupplierProposalStatus.APPROVED);
        } else {
            proposal.setStatus(SupplierProposalStatus.REJECTED);
        }
        proposal.setPendingTaxCode(null);
        proposal.setReviewedBy(TicketAccess.username());
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewComment(request.comment());
        return mapper.toDto(proposals.saveAndFlush(proposal));
    }

    private void checkSupplier(String taxCode) {
        if (suppliers.existsByTaxCode(taxCode)) 
            throw conflict("Mã số thuế đã tồn tại trong danh sách nhà cung cấp.");
    }

    private <T> void validate(T request) {
        var errors = validator.validate(request);
        if (!errors.isEmpty()) 
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errors.stream().map(e -> e.getMessage()).sorted().collect(Collectors.joining("; ")));
    }

    private void requireRole(String... roles) {
        for (String role : roles) 
            if (TicketAccess.hasRole(role)) 
                return;
        throw new AccessDeniedException("Bạn không có quyền thực hiện thao tác này.");
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
