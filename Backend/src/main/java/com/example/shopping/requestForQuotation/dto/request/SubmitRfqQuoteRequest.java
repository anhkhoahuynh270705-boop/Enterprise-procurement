package com.example.shopping.requestForQuotation.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.shopping.requestForQuotation.enums.RfqTechnicalStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SubmitRfqQuoteRequest(
        @NotNull
        LocalDate validUntil,

        @PositiveOrZero
        Integer deliveryDays,

        @Size(max = 1000)
        String paymentTerms,

        @Size(max = 1000)
        String warranty,

        @NotNull
        RfqTechnicalStatus technicalStatus,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal taxAmount,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal shippingCost,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal otherCost,

        @NotEmpty
        List<@NotNull @Valid QuoteItemRequest> items
) {


}
