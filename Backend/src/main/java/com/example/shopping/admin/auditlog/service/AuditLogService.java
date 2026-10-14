package com.example.shopping.admin.auditlog.service;

import com.example.shopping.admin.auditlog.dto.AuditLogDto;
import com.example.shopping.admin.auditlog.entity.AuditLogEntry;
import org.springframework.data.domain.Page;

public interface AuditLogService {
    Page<AuditLogDto> search(
            String module,
            String status,
            String dateRange,
            String keyword,
            int page,
            int size
    );
    void record(AuditLogEntry entry);


}
