package com.example.shopping.supplier.document.dto.response;

public record SupplierDocumentDownloadDto(
    String filename, 
    String contentType, 
    byte[] bytes
) {}
