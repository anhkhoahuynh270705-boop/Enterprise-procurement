package com.example.shopping.integration.kafka.event;

import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.example.shopping.integration.kafka.model.ProcurementEvent;
import com.example.shopping.integration.mail.service.NotificationService;
import com.example.shopping.integration.mail.factory.NotificationServiceFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ProcurementEventListener {

    private final NotificationServiceFactory notificationServiceFactory;

    public ProcurementEventListener(NotificationServiceFactory notificationServiceFactory) {
        this.notificationServiceFactory = notificationServiceFactory;
    }

    @Async
    @EventListener
    public void handleProcurementEvent(ProcurementEvent event) {
        log.info("Nhận sự kiện cho phiếu:", event.getEventType(), event.getTicketCode());
        for (NotificationService service : notificationServiceFactory.getAllServices()) {
            try {
                service.sendNotification(event);
            } catch (Exception e) {
                log.error("Lỗi khi xử lý thông báo kênh:", service.getChannel(), e.getMessage());
            }
        }
    }

    @KafkaListener(topics = ProcurementEventPublisher.TOPIC_PROCUREMENT_EVENTS, groupId = "procurement-group", autoStartup = "false")
    public void handleKafkaMessage(ProcurementEvent event) {
        log.info("Nhận message từ Kafka:", event);
        handleProcurementEvent(event);
    }
}
