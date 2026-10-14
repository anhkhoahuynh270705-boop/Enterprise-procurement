package com.example.shopping.supplier.document.service.impl;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.supplier.document.dto.response.SupplierDocumentDto;
import com.example.shopping.supplier.document.dto.response.SupplierDocumentDownloadDto;
import com.example.shopping.supplier.document.dto.response.SupplierOptionDto;
import com.example.shopping.supplier.document.dto.request.SupplierDocumentReviewRequest;
import com.example.shopping.supplier.document.enums.SupplierDocumentStatus;
import com.example.shopping.supplier.document.service.SupplierDocumentService;
import com.example.shopping.supplier.document.mapper.SupplierDocumentMapper;
import com.example.shopping.supplier.document.entity.SupplierDocument;
import com.example.shopping.supplier.document.entity.SupplierDocumentContent;
import com.example.shopping.supplier.document.repository.SupplierDocumentRepository;
import com.example.shopping.supplier.document.repository.SupplierDocumentContentRepository;
import com.example.shopping.supplier.document.validation.SupplierDocumentValidator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.supplier.repository.SupplierRepository;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SupplierDocumentServiceImpl implements SupplierDocumentService {
    SupplierDocumentRepository documents;
    SupplierDocumentContentRepository contents;
    SupplierRepository suppliers;
    SupplierDocumentMapper mapper;
    Validator validator;

    private boolean reviewer() { 
        return TicketAccess.hasRole("ADMIN") || TicketAccess.hasRole("CHECKER"); 
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierOptionDto> supplierOptions() {
        TicketAccess.requireReader();
        return suppliers.findAll().stream().map(s -> new SupplierOptionDto(s.getId(), s.getCode(), s.getName())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierDocumentDto> list() {
        TicketAccess.requireReader();
        var rows = reviewer() ? documents.findAllByOrderBySubmittedAtDesc()
            : documents.findByStatusOrSubmittedByOrderBySubmittedAtDesc(SupplierDocumentStatus.APPROVED, TicketAccess.username());
        return rows.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public SupplierDocumentDto upload(UUID supplierId, MultipartFile file) throws IOException {
        TicketAccess.requireReader();
        var supplier = suppliers.findById(supplierId)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_NOT_FOUND.ex());
        var validated = SupplierDocumentValidator.validate(file);
        var document = new SupplierDocument();
        document.setSupplier(supplier);
        document.setFilename(validated.filename()); 
        document.setContentType(validated.contentType());
        document.setSize(validated.bytes().length); 
        document.setSubmittedBy(TicketAccess.username());
        document.setSubmittedAt(LocalDateTime.now());
        document.setStatus(TicketAccess.hasRole("ADMIN") ? SupplierDocumentStatus.APPROVED : SupplierDocumentStatus.PENDING);
        if (TicketAccess.hasRole("ADMIN")) {
            document.setReviewedBy(TicketAccess.username()); 
            document.setReviewedAt(document.getSubmittedAt());
        }
        document = documents.saveAndFlush(document);
        var content = new SupplierDocumentContent(); 
        content.setDocument(document); 
        content.setBytes(validated.bytes());
        contents.save(content);
        return mapper.toDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierDocumentDownloadDto download(UUID id) {
        TicketAccess.requireReader();
        var document = documents.findById(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_DOCUMENT_NOT_FOUND.ex());
        if (!reviewer() && document.getStatus() != SupplierDocumentStatus.APPROVED
                && !TicketAccess.username().equals(document.getSubmittedBy())) 
                throw ErrorEnum.ACCESS_DENIED.ex("Không có quyền tải tài liệu này.");
        var content = contents.findById(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_DOCUMENT_CONTENT_NOT_FOUND.ex());
        return new SupplierDocumentDownloadDto(document.getFilename(), document.getContentType(), content.getBytes());
    }

    @Override
    @Transactional
    public SupplierDocumentDto review(UUID id, SupplierDocumentReviewRequest request) {
        if (!reviewer()) 
            throw ErrorEnum.ACCESS_DENIED.ex("Chỉ Checker hoặc Admin được duyệt tài liệu.");
        var errors = validator.validate(request);
        if (!errors.isEmpty()) 
            throw ErrorEnum.SUPPLIER_DOCUMENT_INVALID_REVIEW.ex(errors.iterator().next().getMessage());
        var document = documents.findForReview(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_DOCUMENT_NOT_FOUND.ex());
        if (!TicketAccess.hasRole("ADMIN") && TicketAccess.username().equals(document.getSubmittedBy()))
            throw ErrorEnum.SUPPLIER_DOCUMENT_SELF_REVIEW.ex();
        if (document.getStatus() != SupplierDocumentStatus.PENDING)
            throw ErrorEnum.SUPPLIER_DOCUMENT_PROCESSED.ex();
        document.setStatus(request.approved() ? SupplierDocumentStatus.APPROVED : SupplierDocumentStatus.REJECTED);
        document.setReviewedBy(TicketAccess.username()); document.setReviewedAt(LocalDateTime.now());
        document.setReviewComment(request.comment());
        documents.saveAndFlush(document);
        return mapper.toDto(document);
    }
}
