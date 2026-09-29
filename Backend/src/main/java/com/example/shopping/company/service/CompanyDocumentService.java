package com.example.shopping.company.service;

import com.example.shopping.company.dto.CompanyDocumentDto;
import com.example.shopping.company.entity.CompanyDocument;
import com.example.shopping.company.entity.CompanyDocumentContent;
import com.example.shopping.company.repository.CompanyDocumentContentRepository;
import com.example.shopping.company.repository.CompanyDocumentRepository;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.supplier.document.validation.SupplierDocumentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service 
@RequiredArgsConstructor
public class CompanyDocumentService {

    private final CompanyDocumentRepository documents;
    private final CompanyDocumentContentRepository contents;
    private static final Set<String> CATEGORIES = Set.of("POLICY", "PROCEDURE", "TEMPLATE", "TRAINING", "OTHER");

    @Transactional(readOnly = true)
    public List<CompanyDocumentDto> list() {
        TicketAccess.requireReader();
        return documents.findByDeletedFalseOrderByUploadedAtDesc().stream().map(CompanyDocumentDto::from).toList();
    }

    @Transactional
    public CompanyDocumentDto upload(String title, String category, String description, MultipartFile file) throws IOException {
        requireAdmin();
        title = Objects.toString(title, "").strip();
        description = Objects.toString(description, "").strip();
        if (title.isEmpty() || title.length() > 200 || description.length() > 2000
                || category == null || !CATEGORIES.contains(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tiêu đề, danh mục hoặc mô tả không hợp lệ.");
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
        return CompanyDocumentDto.from(doc);
    }

    public record Download(String filename, String contentType, byte[] bytes) {}

    @Transactional(readOnly = true)
    public Download download(UUID id) {
        TicketAccess.requireReader();
        var doc = documents.findByIdAndDeletedFalse(id).orElseThrow(CompanyDocumentService::missing);
        var content = contents.findById(id).orElseThrow(CompanyDocumentService::missing);
        return new Download(doc.getFilename(), doc.getContentType(), content.getBytes());
    }

    @Transactional
    public void delete(UUID id) {
        requireAdmin();
        var doc = documents.findByIdAndDeletedFalse(id).orElseThrow(CompanyDocumentService::missing);
        doc.setDeleted(true);
        documents.save(doc);
    }

    private static void requireAdmin() {
        if (!TicketAccess.hasRole("ADMIN")) 
        throw new AccessDeniedException("Chỉ Admin được quản lý tài liệu công ty.");
    }
    private static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài liệu công ty.");
    }
}
