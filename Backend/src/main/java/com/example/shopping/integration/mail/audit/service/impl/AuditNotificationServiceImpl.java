package com.example.shopping.integration.mail.audit.service.impl;

import com.example.shopping.integration.mail.audit.AuditNotificationService;
import org.springframework.stereotype.Service;
import com.example.shopping.integration.kafka.model.ProcurementEvent;
import com.example.shopping.integration.mail.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditNotificationServiceImpl implements AuditNotificationService {

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.AUDIT_LOG;
    }

    @Override
    public void sendNotification(ProcurementEvent event) {
        log.info("Ghi nhận lịch sử kiểm toán:",
                event.getTicketCode(), event.getEventType(), event.getMakerUsername(),
                event.getCheckerUsername() != null ? event.getCheckerUsername() : "N/A",
                event.getTotalAmount(), event.getCurrency(), event.getTimestamp());
    }
}
