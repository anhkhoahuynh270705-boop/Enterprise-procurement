package com.example.shopping.supplier.dto.request;

import java.util.UUID;

import com.example.shopping.supplier.enums.SupplierStatus;

public record SupplierSearchCriteria(
        String keyword,
        SupplierStatus status,
        UUID supplierId,
        String supplierCode,
        String contactPerson,
        String taxCode,
        String email,
        String phone,
        String city
) {}
