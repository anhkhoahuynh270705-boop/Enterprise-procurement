package com.example.shopping.supplier.report.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.example.shopping.supplier.dto.response.SupplierResponseDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierImportResultDto {

    private int totalRows;
    private int successRows;
    private int errorRows;
    private String message;

    @Builder.Default
    private List<SupplierResponseDto> createdSuppliers = new ArrayList<>();

    @Builder.Default
    private List<SupplierImportRowErrorDto> errors = new ArrayList<>();
}
