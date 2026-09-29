package com.example.shopping.supplier.report.dto.response;

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
public class SupplierImportRowErrorDto {

    private int rowNumber;
    private String field;
    private String errorMessage;
}
