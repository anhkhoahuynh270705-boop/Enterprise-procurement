package com.example.shopping.requestForQuotation.kafka.event;

import java.util.UUID;

public record RfqInitialApprovalEvent(
        UUID rfqId,
        String approvedBy
) {
}
