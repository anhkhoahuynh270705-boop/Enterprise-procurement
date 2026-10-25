package com.example.shopping.requestForQuotation.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record SelectRfqWinnersRequest(
        UUID supplierId,

        @Size(max = 2000)
        String reason,

        @Valid
        List<ItemWinnerRequest> itemWinners,

        boolean submitForApproval) {


}
