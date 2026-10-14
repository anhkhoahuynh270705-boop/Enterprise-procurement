package com.example.shopping.supplier.contract.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.shopping.supplier.contract.enums.SupplierContractStatus;

public record SupplierContractDto(
        UUID id,
        String contractNumber,
        String title,
        UUID supplierId,
        String supplierCode,
        String supplierName,
        LocalDate signedDate,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal amount,
        String currency,
        SupplierContractStatus status,
        String notes,
        String attachmentName,
        String attachmentContentType,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
