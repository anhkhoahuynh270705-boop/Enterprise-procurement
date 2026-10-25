package com.example.shopping.integration.kafka.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.shopping.integration.kafka.model.ProcurementEvent;
import com.example.shopping.integration.mail.factory.NotificationServiceFactory;
import com.example.shopping.integration.mail.service.NotificationService;

import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ProcurementEventListener {

    NotificationServiceFactory notificationServiceFactory;

    public ProcurementEventListener(NotificationServiceFactory notificationServiceFactory) {
        this.notificationServiceFactory = notificationServiceFactory;
    }

    public void processProcurementEvent(ProcurementEvent event) {
        log.info("Nhận sự kiện cho phiếu:", event.getEventType(), event.getTicketCode());
        for (NotificationService service : notificationServiceFactory.getAllServices()) {
            try {
                service.sendNotification(event);
            } catch (Exception e) {
                log.error("Lỗi khi xử lý thông báo kênh:", service.getChannel(), e.getMessage());
            }
        }
    }

    @KafkaListener(topics = ProcurementEventPublisher.TOPIC_PROCUREMENT_EVENTS, groupId = "procurement-group", autoStartup = "true")
    public void handleKafkaMessage(ProcurementEvent event) {
        log.info("Nhận message từ Kafka:", event);
        processProcurementEvent(event);
    }
}
