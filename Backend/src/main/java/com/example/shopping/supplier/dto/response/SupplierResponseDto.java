package com.example.shopping.supplier.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.shopping.supplier.enums.SupplierStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponseDto {

    private UUID id;
    private String code;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String taxCode;
    private String bankAccount;
    private String bankName;
    private String notes;
    private SupplierStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
