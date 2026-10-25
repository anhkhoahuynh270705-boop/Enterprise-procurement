package com.example.shopping.requestForQuotation.kafka.listener;

import com.example.shopping.requestForQuotation.kafka.publisher.RfqEventPublisher;
import com.example.shopping.requestForQuotation.kafka.event.RfqApprovedKafkaEvent;
import com.example.shopping.requestForQuotation.service.SupplierInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RfqApprovalKafkaListener {

    private final SupplierInvitationService invitationService;

    @KafkaListener(
            topics = RfqEventPublisher.TOPIC_RFQ_APPROVED,
            groupId = "rfq-invitation-group")
    public void sendSupplierInvitations(RfqApprovedKafkaEvent event) {
        log.info("Nhận sự kiện RFQ đã duyệt từ Kafka:",
                event.rfqId(), event.approvedBy());
        invitationService.sendInvitationsForApprovedRfq(event.rfqId(), event.approvedBy());
    }
}
