package com.example.shopping.admin.auditlog.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

public record AuditLogDto(
        UUID id,
        String logCode,
        LocalDateTime occurredAt,
        String username,
        String userFullName,
        String action,
        String module,
        String moduleLabel,
        String status,
        String description,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        Details details
) {
    public record Details(
            String userAgent,
            String method,
            Integer statusCode,
            Integer responseTimeMs,
            String payloadSummary
    ) { }
}
