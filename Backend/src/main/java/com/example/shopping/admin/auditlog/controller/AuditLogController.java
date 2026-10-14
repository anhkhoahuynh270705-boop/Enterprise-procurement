package com.example.shopping.admin.auditlog.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopping.admin.auditlog.dto.AuditLogDto;
import com.example.shopping.admin.auditlog.service.AuditLogService;
import com.example.shopping.common.dto.response.ApiResponse;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Truy vấn nhật ký hoạt động — chỉ ADMIN.
 */
@RestController
@RequestMapping("/api/admin/audit-logs")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuditLogController {

    AuditLogService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogDto>>> search(
            @RequestParam(defaultValue = "ALL")   String module,
            @RequestParam(defaultValue = "ALL")   String status,
            @RequestParam(defaultValue = "TODAY") String dateRange,
            @RequestParam(defaultValue = "")      String keyword,
            @RequestParam(defaultValue = "0")     int page,
            @RequestParam(defaultValue = "20")    int size
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        service.search(module, status, dateRange, keyword, page, size),
                        "Tải nhật ký hoạt động thành công."));
    }
}
