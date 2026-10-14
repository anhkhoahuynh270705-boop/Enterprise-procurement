package com.example.shopping.company.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyDocumentDto(
        UUID id,
        String title,
        String category,
        String description,
        String filename,
        String contentType,
        long size,
        String uploadedBy,
        LocalDateTime uploadedAt) {
}
