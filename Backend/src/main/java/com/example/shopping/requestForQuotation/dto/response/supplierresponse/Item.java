package com.example.shopping.requestForQuotation.dto.response.supplierresponse;

import java.util.UUID;

public record Item(
        UUID rfqItemId,
        String sku,
        String itemName,
        Integer quantity,
        String unit
) {
}
