package com.example.shopping.purchaseorder.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePurchaseOrderRequest {

    @NotNull
    private UUID ticketId;

    @NotNull
    private UUID supplierId;

    private LocalDate expectedDeliveryDate;

    @Size(max = 1000)
    private String notes;

    @NotEmpty
    private List<@NotNull UUID> procurementItemIds;
}
