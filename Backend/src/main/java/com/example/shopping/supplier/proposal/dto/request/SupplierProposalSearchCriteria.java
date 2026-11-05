package com.example.shopping.supplier.proposal.dto.request;

import com.example.shopping.supplier.proposal.enums.SupplierProposalStatus;

public record SupplierProposalSearchCriteria(String keyword, SupplierProposalStatus status) {
}
