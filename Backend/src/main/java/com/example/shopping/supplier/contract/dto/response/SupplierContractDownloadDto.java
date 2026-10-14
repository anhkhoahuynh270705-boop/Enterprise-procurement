package com.example.shopping.supplier.contract.dto.response;

public record SupplierContractDownloadDto(
    String filename, 
    String contentType, 
    byte[] bytes
    ) {}
