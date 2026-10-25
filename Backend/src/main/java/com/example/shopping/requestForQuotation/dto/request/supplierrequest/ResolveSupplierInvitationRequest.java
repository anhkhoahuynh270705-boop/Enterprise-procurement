package com.example.shopping.requestForQuotation.dto.request.supplierrequest;

import jakarta.validation.constraints.NotBlank;

public record ResolveSupplierInvitationRequest (
        @NotBlank
        String token
)
{}
