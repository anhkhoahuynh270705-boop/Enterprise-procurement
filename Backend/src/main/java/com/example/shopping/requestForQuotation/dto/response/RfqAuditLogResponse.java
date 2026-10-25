package com.example.shopping.requestForQuotation.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record RfqAuditLogResponse(
        UUID id,
        String actorUsername,
        String action,
        Map<String, Object> details,
        LocalDateTime createdAt) {
}
