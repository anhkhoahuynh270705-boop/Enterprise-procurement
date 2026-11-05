package com.example.shopping.integration.mail.service;

import com.example.shopping.integration.mail.enums.NotificationChannel;
import com.example.shopping.procurement.kafka.model.ProcurementEvent;

public interface NotificationService {

    NotificationChannel getChannel();

    void sendNotification(ProcurementEvent event);
}
