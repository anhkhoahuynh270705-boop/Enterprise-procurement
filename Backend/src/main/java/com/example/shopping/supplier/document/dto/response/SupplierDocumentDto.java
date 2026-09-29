package com.example.shopping.supplier.document.dto.response;

import com.example.shopping.supplier.document.enums.SupplierDocumentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SupplierDocumentDto(
    UUID id, 
    UUID supplierId, 
    String supplierName, 
    String filename,
    String contentType, 
    long size, 
    SupplierDocumentStatus status, 
    String submittedBy, 
    LocalDateTime submittedAt,
    String reviewedBy, 
    LocalDateTime reviewedAt, 
    String reviewComment) 
    { }
