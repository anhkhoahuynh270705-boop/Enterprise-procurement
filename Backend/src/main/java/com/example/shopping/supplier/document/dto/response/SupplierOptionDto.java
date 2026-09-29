package com.example.shopping.supplier.document.dto.response;

import java.util.UUID;

public record SupplierOptionDto(
    UUID id, 
    String code, 
    String name
) {}
