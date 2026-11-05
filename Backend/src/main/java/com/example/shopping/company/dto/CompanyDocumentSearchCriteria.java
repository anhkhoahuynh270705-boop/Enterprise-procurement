package com.example.shopping.company.dto;

import java.time.LocalDate;

public record CompanyDocumentSearchCriteria(
        String keyword,
        String category,
        String uploadedBy,
        LocalDate uploadedFrom,
        LocalDate uploadedTo,
        String fileType) {
}
