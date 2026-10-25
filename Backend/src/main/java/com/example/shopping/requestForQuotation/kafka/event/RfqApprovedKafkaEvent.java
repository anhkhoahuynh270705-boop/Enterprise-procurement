package com.example.shopping.requestForQuotation.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** Kafka message emitted after a checker approves an RFQ. */
public record RfqApprovedKafkaEvent(
        UUID rfqId,
        String approvedBy,
        LocalDateTime approvedAt
) {
}
