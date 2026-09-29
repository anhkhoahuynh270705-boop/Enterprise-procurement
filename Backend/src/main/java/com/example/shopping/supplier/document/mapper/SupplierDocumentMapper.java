package com.example.shopping.supplier.document.mapper;

import com.example.shopping.supplier.document.dto.response.SupplierDocumentDto;
import com.example.shopping.supplier.document.entity.SupplierDocument;

public final class SupplierDocumentMapper {
    private SupplierDocumentMapper() { }

    public static SupplierDocumentDto toDto(SupplierDocument document) {
        return new SupplierDocumentDto(
            document.getId(), 
            document.getSupplier().getId(),
            document.getSupplier().getName(), 
            document.getFilename(), 
            document.getContentType(),
            document.getSize(), 
            document.getStatus(), 
            document.getSubmittedBy(), 
            document.getSubmittedAt(),
            document.getReviewedBy(), 
            document.getReviewedAt(), 
            document.getReviewComment());
    }
}
