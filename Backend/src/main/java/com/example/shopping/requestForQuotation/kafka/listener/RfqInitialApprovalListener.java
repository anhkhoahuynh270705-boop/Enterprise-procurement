package com.example.shopping.requestForQuotation.kafka.listener;

import com.example.shopping.requestForQuotation.kafka.event.RfqInitialApprovalEvent;
import com.example.shopping.requestForQuotation.kafka.publisher.RfqEventPublisher;
import com.example.shopping.requestForQuotation.kafka.event.RfqApprovedKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RfqInitialApprovalListener {

    private final RfqEventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterApproval(RfqInitialApprovalEvent event) {
        eventPublisher.publishRfqApproved(new RfqApprovedKafkaEvent(
                event.rfqId(), event.approvedBy(), java.time.LocalDateTime.now()));
    }
}
