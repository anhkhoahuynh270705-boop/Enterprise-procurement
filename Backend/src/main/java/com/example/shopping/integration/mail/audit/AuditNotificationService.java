package com.example.shopping.integration.mail.audit;

import com.example.shopping.integration.mail.enums.NotificationChannel;
import com.example.shopping.integration.mail.service.NotificationService;
import com.example.shopping.procurement.kafka.model.ProcurementEvent;

public interface AuditNotificationService extends NotificationService {
    NotificationChannel getChannel();
    void sendNotification(ProcurementEvent event);
}
