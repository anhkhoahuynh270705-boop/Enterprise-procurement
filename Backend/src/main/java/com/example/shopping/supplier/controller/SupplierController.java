package com.example.shopping.supplier.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopping.common.dto.response.ApiResponse;
import com.example.shopping.supplier.dto.request.CreateSupplierRequestDto;
import com.example.shopping.supplier.dto.request.SupplierSearchCriteria;
import com.example.shopping.supplier.dto.request.UpdateSupplierRequestDto;
import com.example.shopping.supplier.dto.response.SupplierResponseDto;
import com.example.shopping.supplier.enums.SupplierStatus;
import com.example.shopping.supplier.service.SupplierService;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierController {

    SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<SupplierResponseDto>> getAllSuppliers(
            @RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return ResponseEntity.ok(supplierService.searchSuppliers(keyword));
        }
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponseDto> getSupplierById(@PathVariable UUID id) {
        return supplierService.getSupplierById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<SupplierResponseDto> getSupplierByCode(@PathVariable String code) {
        return supplierService.getSupplierByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<SupplierResponseDto>>> getSupplierPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SupplierStatus status,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) String supplierCode,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String taxCode,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        SupplierSearchCriteria criteria = new SupplierSearchCriteria(
                keyword, status, supplierId, supplierCode,
                contactPerson, taxCode, email, phone, city);

        Page<SupplierResponseDto> result = supplierService.searchPage(
                criteria,
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Direction.ASC, "name")
                                .and(Sort.by(Sort.Direction.ASC, "id"))));

        return ResponseEntity.ok(
                ApiResponse.success(result, "Lấy danh sách nhà cung cấp thành công"));
    }


    @PostMapping
    public ResponseEntity<SupplierResponseDto> createSupplier(
            @Valid @RequestBody CreateSupplierRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplierService.createSupplier(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierResponseDto> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequestDto request) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable UUID id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
