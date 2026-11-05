package com.example.shopping.workflow.camunda.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Common Kafka event for completed Checker reviews across procurement workflows. */
public record WorkflowReviewEvent(
        String entityType,
        UUID entityId,
        String referenceCode,
        String title,
        String submittedBy,
        String reviewedBy,
        String outcome,
        String comment,
        BigDecimal amount,
        LocalDateTime reviewedAt) {
}
