package com.example.shopping.audit.service;

import com.example.shopping.integration.kafka.model.ProcurementEvent;
import com.example.shopping.integration.mail.enums.NotificationChannel;
import com.example.shopping.integration.mail.service.NotificationService;

public interface AuditNotificationService extends NotificationService {
    NotificationChannel getChannel();
    void sendNotification(ProcurementEvent event);
}
