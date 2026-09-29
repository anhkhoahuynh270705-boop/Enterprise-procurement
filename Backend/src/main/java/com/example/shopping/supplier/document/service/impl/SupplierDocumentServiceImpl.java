package com.example.shopping.supplier.document.service.impl;

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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.supplier.repository.SupplierRepository;

@Service @RequiredArgsConstructor
public class SupplierDocumentServiceImpl implements SupplierDocumentService {
    private final SupplierDocumentRepository documents;
    private final SupplierDocumentContentRepository contents;
    private final SupplierRepository suppliers;
    private final Validator validator;

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
        return rows.stream().map(SupplierDocumentMapper::toDto).toList();
    }

    @Override
    @Transactional
    public SupplierDocumentDto upload(UUID supplierId, MultipartFile file) throws IOException {
        TicketAccess.requireReader();
        var supplier = suppliers.findById(supplierId).orElseThrow(() -> missing("Không tìm thấy nhà cung cấp."));
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
        var content = new SupplierDocumentContent(); content.setDocument(document); content.setBytes(validated.bytes());
        contents.save(content);
        return SupplierDocumentMapper.toDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierDocumentDownloadDto download(UUID id) {
        TicketAccess.requireReader();
        var document = documents.findById(id).orElseThrow(() -> missing("Không tìm thấy tài liệu."));
        if (!reviewer() && document.getStatus() != SupplierDocumentStatus.APPROVED
                && !TicketAccess.username().equals(document.getSubmittedBy())) 
                throw new AccessDeniedException("Không có quyền tải tài liệu này.");
        var content = contents.findById(id).orElseThrow(() -> missing("Không tìm thấy nội dung tài liệu."));
        return new SupplierDocumentDownloadDto(document.getFilename(), document.getContentType(), content.getBytes());
    }

    @Override
    @Transactional
    public SupplierDocumentDto review(UUID id, SupplierDocumentReviewRequest request) {
        if (!reviewer()) 
            throw new AccessDeniedException("Chỉ Checker hoặc Admin được duyệt tài liệu.");
        var errors = validator.validate(request);
        if (!errors.isEmpty()) 
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errors.iterator().next().getMessage());
        var document = documents.findForReview(id)
                                .orElseThrow(() -> missing("Không tìm thấy tài liệu."));
        if (!TicketAccess.hasRole("ADMIN") && TicketAccess.username().equals(document.getSubmittedBy()))
            throw new AccessDeniedException("Checker không được tự duyệt đề xuất của mình.");
        if (document.getStatus() != SupplierDocumentStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tài liệu đã được xử lý. Vui lòng tải lại danh sách.");
        document.setStatus(request.approved() ? SupplierDocumentStatus.APPROVED : SupplierDocumentStatus.REJECTED);
        document.setReviewedBy(TicketAccess.username()); document.setReviewedAt(LocalDateTime.now());
        document.setReviewComment(request.comment());
        documents.saveAndFlush(document);
        return SupplierDocumentMapper.toDto(document);
    }

    private ResponseStatusException missing(String message) { 
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
}
