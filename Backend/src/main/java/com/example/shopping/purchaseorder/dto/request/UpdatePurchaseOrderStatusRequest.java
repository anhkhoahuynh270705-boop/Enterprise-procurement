package com.example.shopping.purchaseorder.dto.request;

import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePurchaseOrderStatusRequest {

    @NotNull
    private PurchaseOrderStatus status;
}
