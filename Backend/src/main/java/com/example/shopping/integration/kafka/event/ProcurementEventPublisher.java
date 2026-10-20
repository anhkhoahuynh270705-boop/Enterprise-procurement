package com.example.shopping.integration.kafka.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.example.shopping.integration.kafka.model.ProcurementEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProcurementEventPublisher {

    public static final String TOPIC_PROCUREMENT_EVENTS = "procurement-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public ProcurementEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    public void publishEvent(ProcurementEvent event) {
        log.info("Event cho phiếu:", event.getEventType(), event.getTicketCode());

        kafkaTemplate.send(TOPIC_PROCUREMENT_EVENTS, event.getTicketCode(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Không thể gửi sự kiện vào Kafka",
                                TOPIC_PROCUREMENT_EVENTS, error.getMessage(), error);
                        return;
                    }
                    log.info("Đã gửi sự kiện vào Kafka", TOPIC_PROCUREMENT_EVENTS);
                });
    }
}
