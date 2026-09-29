package com.example.shopping.procurement.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

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
public class ProcurementItemDto {

    private UUID id;

    private String itemCode;

    private String itemName;

    private String category;


    private Integer quantity;

    private String unit;


    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private String supplierName;

    private String notes;
}
