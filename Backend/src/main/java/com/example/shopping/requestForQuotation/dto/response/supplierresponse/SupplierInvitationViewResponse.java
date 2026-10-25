package com.example.shopping.requestForQuotation.dto.response.supplierresponse;

import java.time.LocalDateTime;
import java.util.List;

public record SupplierInvitationViewResponse(
        String rfqCode,
        String title,
        LocalDateTime deadline,
        String deliveryLocation,
        String deliveryWindow,
        String currency,
        List<Item> items
) {
}
