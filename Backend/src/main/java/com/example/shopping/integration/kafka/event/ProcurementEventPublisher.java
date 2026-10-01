package com.example.shopping.integration.kafka.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.example.shopping.integration.kafka.model.ProcurementEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProcurementEventPublisher {

    public static final String TOPIC_PROCUREMENT_EVENTS = "procurement-events";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public ProcurementEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            @Autowired(required = false) 
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Async
    public void publishEvent(ProcurementEvent event) {
        log.info("Event cho phiếu:", event.getEventType(), event.getTicketCode());

        // Thử gửi qua Kafka Message Queue
        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TOPIC_PROCUREMENT_EVENTS, event.getTicketCode(), event);
                log.info("Đã đẩy message vào Kafka topic.", TOPIC_PROCUREMENT_EVENTS);
            } catch (Exception e) {
                log.warn("Không thể gửi vào Kafka topic: ",
                        TOPIC_PROCUREMENT_EVENTS, e.getMessage());
            }
        }
        applicationEventPublisher.publishEvent(event);
    }
}
