package com.example.shopping.procurement.report.dto.response;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultDto {

    private int totalRows;

    private int successRows;

    private int errorRows;

    private String message;

    private ProcurementTicketResponseDto createdTicket;

    @Builder.Default
    private List<ImportRowErrorDto> errors = new ArrayList<>();
}
