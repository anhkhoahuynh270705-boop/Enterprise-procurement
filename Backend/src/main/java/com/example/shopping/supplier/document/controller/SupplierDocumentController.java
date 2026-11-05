package com.example.shopping.supplier.document.controller;

import com.example.shopping.supplier.document.dto.response.SupplierDocumentDto;
import com.example.shopping.supplier.document.dto.response.SupplierOptionDto;
import com.example.shopping.supplier.document.dto.request.SupplierDocumentReviewRequest;
import com.example.shopping.supplier.document.dto.request.SupplierDocumentSearchCriteria;
import com.example.shopping.supplier.document.enums.SupplierDocumentStatus;
import com.example.shopping.supplier.document.service.SupplierDocumentService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController 
@RequestMapping("/api/supplier-documents") 
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SupplierDocumentController {
    SupplierDocumentService service;
    @GetMapping 
    public List<SupplierDocumentDto> list() { 
        return service.list(); 
    }

    @GetMapping("/page")
    public Page<SupplierDocumentDto> search(
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) SupplierDocumentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String submittedBy,
            @RequestParam(required = false) String reviewedBy,
            @RequestParam(required = false) String reviewComment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate submittedTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reviewedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reviewedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var criteria = new SupplierDocumentSearchCriteria(
                supplierId, status, keyword, submittedBy, reviewedBy, reviewComment,
                submittedFrom, submittedTo, reviewedFrom, reviewedTo);
        return service.search(criteria, PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by(Sort.Direction.DESC, "id"))));
    }

    @GetMapping("/suppliers") 
    public List<SupplierOptionDto> suppliers() { 
        return service.supplierOptions(); 
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) 
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierDocumentDto upload(@RequestParam UUID supplierId, @RequestParam MultipartFile file) throws IOException {
        return service.upload(supplierId, file);
    }
    
    @GetMapping("/{id}/download") 
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        var download = service.download(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.contentType()))
            .contentLength(download.bytes().length).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.filename(), StandardCharsets.UTF_8).build().toString())
            .body(download.bytes());
    }
    @PostMapping("/{id}/review") public SupplierDocumentDto review(@PathVariable UUID id, @Valid @RequestBody SupplierDocumentReviewRequest request) {
        return service.review(id, request);
    }
}
