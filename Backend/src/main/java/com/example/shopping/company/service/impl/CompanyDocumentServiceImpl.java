package com.example.shopping.company.service.impl;

import com.example.shopping.company.dto.CompanyDocumentDto;
import com.example.shopping.company.entity.CompanyDocument;
import com.example.shopping.company.entity.CompanyDocumentContent;
import com.example.shopping.company.mapper.CompanyDocumentMapper;
import com.example.shopping.company.repository.CompanyDocumentContentRepository;
import com.example.shopping.company.repository.CompanyDocumentRepository;
import com.example.shopping.company.service.CompanyDocumentService;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.supplier.document.validation.SupplierDocumentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.example.shopping.common.enums.ErrorEnum;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyDocumentServiceImpl implements CompanyDocumentService {

    CompanyDocumentRepository documents;
    CompanyDocumentContentRepository contents;
    CompanyDocumentMapper mapper;
    private static final Set<String> CATEGORIES = Set.of("POLICY", "PROCEDURE", "TEMPLATE", "TRAINING", "OTHER");

    @Transactional(readOnly = true)
    public List<CompanyDocumentDto> list() {
        TicketAccess.requireReader();
        return documents.findByDeletedFalseOrderByUploadedAtDesc().stream().map(mapper::toDto).toList();
    }

    @Transactional
    public CompanyDocumentDto upload(String title, String category, String description, MultipartFile file) throws IOException {
        requireAdmin();
        title = Objects.toString(title, "").strip();
        description = Objects.toString(description, "").strip();
        if (title.isEmpty() || title.length() > 200 || description.length() > 2000
                || category == null || !CATEGORIES.contains(category)) {
            throw ErrorEnum.COMPANY_DOCUMENT_INVALID.ex();
        }
        var validated = SupplierDocumentValidator.validate(file);
        var doc = new CompanyDocument();
        doc.setTitle(title);
        doc.setCategory(category);
        doc.setDescription(description);
        doc.setFilename(validated.filename());
        doc.setContentType(validated.contentType());
        doc.setSize(validated.bytes().length);
        doc.setUploadedBy(TicketAccess.username());
        doc.setUploadedAt(LocalDateTime.now());
        doc = documents.saveAndFlush(doc);
        var content = new CompanyDocumentContent();
        content.setDocument(doc); content.setBytes(validated.bytes());
        contents.save(content);
        return mapper.toDto(doc);
    }

    @Transactional(readOnly = true)
    public Download download(UUID id) {
        TicketAccess.requireReader();
        var doc = documents.findByIdAndDeletedFalse(id).orElseThrow(ErrorEnum.COMPANY_DOCUMENT_NOT_FOUND::ex);
        var content = contents.findById(id).orElseThrow(ErrorEnum.COMPANY_DOCUMENT_NOT_FOUND::ex);
        return new Download(doc.getFilename(), doc.getContentType(), content.getBytes());
    }

    @Transactional
    public void delete(UUID id) {
        requireAdmin();
        var doc = documents.findByIdAndDeletedFalse(id).orElseThrow(ErrorEnum.COMPANY_DOCUMENT_NOT_FOUND::ex);
        doc.setDeleted(true);
        documents.save(doc);
    }

    private static void requireAdmin() {
        if (!TicketAccess.hasRole("ADMIN"))
            throw ErrorEnum.ACCESS_DENIED.ex("Chỉ Admin được quản lý tài liệu công ty.");
    }
}
