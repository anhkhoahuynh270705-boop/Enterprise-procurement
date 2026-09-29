package com.example.shopping.supplier.document.controller;

import com.example.shopping.supplier.document.dto.response.SupplierDocumentDto;
import com.example.shopping.supplier.document.dto.response.SupplierOptionDto;
import com.example.shopping.supplier.document.dto.request.SupplierDocumentReviewRequest;
import com.example.shopping.supplier.document.service.SupplierDocumentService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/supplier-documents") @RequiredArgsConstructor
public class SupplierDocumentController {
    private final SupplierDocumentService service;
    @GetMapping public List<SupplierDocumentDto> list() { return service.list(); }
    @GetMapping("/suppliers") public List<SupplierOptionDto> suppliers() { return service.supplierOptions(); }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public SupplierDocumentDto upload(@RequestParam UUID supplierId, @RequestParam MultipartFile file) throws IOException {
        return service.upload(supplierId, file);
    }
    @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@PathVariable UUID id) {
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
