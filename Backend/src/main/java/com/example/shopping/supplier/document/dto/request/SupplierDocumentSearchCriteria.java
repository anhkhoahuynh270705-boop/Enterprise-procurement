package com.example.shopping.supplier.document.dto.request;

import com.example.shopping.supplier.document.enums.SupplierDocumentStatus;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierDocumentSearchCriteria(
        UUID supplierId,
        SupplierDocumentStatus status,
        String keyword,
        String submittedBy,
        String reviewedBy,
        String reviewComment,
        LocalDate submittedFrom,
        LocalDate submittedTo,
        LocalDate reviewedFrom,
        LocalDate reviewedTo) {
}
