package com.example.shopping.supplier.contract.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.shopping.admin.auditlog.annotation.Audited;
import com.example.shopping.supplier.contract.dto.request.SupplierContractRequest;
import com.example.shopping.supplier.contract.dto.response.SupplierContractDto;
import com.example.shopping.supplier.contract.service.SupplierContractService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/supplier-contracts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SupplierContractController {

    SupplierContractService service;

    @GetMapping
    public List<SupplierContractDto> list() {
        return service.list();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(
        action = "Tạo hợp đồng nhà cung cấp",
        module = "CONTRACT",
        moduleLabel = "Hợp đồng",
        description = "Tạo mới hợp đồng nhà cung cấp"
    )
    public ResponseEntity<SupplierContractDto> create(
            @Valid @RequestPart("contract") SupplierContractRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, file));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Audited(
        action = "Cập nhật hợp đồng nhà cung cấp",
        module = "CONTRACT",
        moduleLabel = "Hợp đồng",
        description = "Chỉnh sửa thông tin hợp đồng nhà cung cấp"
    )
    public SupplierContractDto update(
            @PathVariable UUID id,
            @Valid @RequestPart("contract") SupplierContractRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        return service.update(id, request, file);
    }

    @DeleteMapping("/{id}")
    @Audited(
        action = "Xóa hợp đồng nhà cung cấp",
        module = "CONTRACT",
        moduleLabel = "Hợp đồng",
        description = "Xóa hợp đồng nhà cung cấp"
    )
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        var download = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.bytes().length)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.filename(), StandardCharsets.UTF_8)
                        .build().toString())
                .body(download.bytes());
    }
}
