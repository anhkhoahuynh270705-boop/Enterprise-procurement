package com.example.shopping.procurement.report.dto.response;

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
public class ImportRowErrorDto {

    private int rowNumber;

    private String field;

    private String errorMessage;
}
