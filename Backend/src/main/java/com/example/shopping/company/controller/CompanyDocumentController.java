package com.example.shopping.company.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.shopping.company.dto.CompanyDocumentDto;
import com.example.shopping.company.dto.CompanyDocumentSearchCriteria;
import com.example.shopping.company.service.CompanyDocumentService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController 
@RequestMapping("/api/company-documents") 
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CompanyDocumentController {
    CompanyDocumentService service;

    @GetMapping public List<CompanyDocumentDto> list() { 
        return service.list(); 
    }

    @GetMapping("/page")
    public Page<CompanyDocumentDto> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) LocalDate uploadedFrom,
            @RequestParam(required = false) LocalDate uploadedTo,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var criteria = new CompanyDocumentSearchCriteria(
                keyword, category, uploadedBy, uploadedFrom, uploadedTo, fileType);
        return service.search(criteria, PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "uploadedAt")
                        .and(Sort.by(Sort.Direction.DESC, "id"))));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) 
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyDocumentDto upload(@RequestParam String title, @RequestParam String category,
            @RequestParam(defaultValue = "") String description, @RequestParam MultipartFile file) 
            throws IOException {
        return service.upload(title, category, description, file);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        var file = service.download(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
            .contentLength(file.bytes().length).cacheControl(CacheControl.noStore())
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8).build().toString())
            .body(file.bytes());
    }

    @DeleteMapping("/{id}") 
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { 
        service.delete(id); 
    }
}
