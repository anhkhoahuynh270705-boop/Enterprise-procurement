package com.example.shopping.company.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.shopping.company.dto.CompanyDocumentDto;
import com.example.shopping.company.service.CompanyDocumentService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController 
@RequestMapping("/api/company-documents") 
@RequiredArgsConstructor
public class CompanyDocumentController {
    private final CompanyDocumentService service;

    @GetMapping public List<CompanyDocumentDto> list() { 
        return service.list(); 
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
