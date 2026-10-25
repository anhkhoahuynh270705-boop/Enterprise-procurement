package com.example.shopping.requestForQuotation.dto.request;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRfqRequest(
        @NotNull
        UUID procurementTicketId,

        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 100)
        String department,

        @NotNull
        @Future
        LocalDateTime deadline,

        @NotNull
        @Min(1)
        @Max(365)
        Integer quoteValidityDays,

        @Size(max = 500)
        String deliveryLocation,

        @Size(max = 255)
        String deliveryWindow,

        @NotEmpty
        List<@NotNull UUID> supplierIds,

        @Size(max = 2000)
        String exceptionReason,

        @NotEmpty
        List<@NotNull @Valid RfqItemRequest> items,

        @Size(max = 5)
        List<@NotNull @Valid RfqScoreCriterionRequest> scoreCriteria,
        boolean submitForApproval
) { }
