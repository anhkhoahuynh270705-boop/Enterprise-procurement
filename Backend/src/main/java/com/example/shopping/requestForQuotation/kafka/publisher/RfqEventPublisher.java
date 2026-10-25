package com.example.shopping.requestForQuotation.kafka.publisher;

import com.example.shopping.requestForQuotation.kafka.event.RfqApprovedKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RfqEventPublisher {

    public static final String TOPIC_RFQ_APPROVED = "rfq-approved-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishRfqApproved(RfqApprovedKafkaEvent event) {
        kafkaTemplate.send(TOPIC_RFQ_APPROVED, event.rfqId().toString(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Không thể gửi sự kiện RFQ đã duyệt vào Kafka: rfqId={}",
                                event.rfqId(), error);
                        return;
                    }
                    log.info("Đã gửi sự kiện RFQ đã duyệt vào Kafka: rfqId={}, topic={}",
                            event.rfqId(), TOPIC_RFQ_APPROVED);
                });
    }
}
