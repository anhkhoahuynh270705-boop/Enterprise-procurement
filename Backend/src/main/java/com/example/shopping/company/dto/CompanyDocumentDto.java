package com.example.shopping.company.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.shopping.company.entity.CompanyDocument;

public record CompanyDocumentDto(
    UUID id, 
    String title, 
    String category, 
    String description,       
    String filename, 
    String contentType, 
    long size, 
    String uploadedBy, 
    LocalDateTime uploadedAt
    ) {
    public static CompanyDocumentDto from(CompanyDocument doc) {
        return new CompanyDocumentDto(
            doc.getId(), 
            doc.getTitle(), 
            doc.getCategory(), 
            doc.getDescription(),
            doc.getFilename(), 
            doc.getContentType(), 
            doc.getSize(), 
            doc.getUploadedBy(), 
            doc.getUploadedAt());
    }
}
