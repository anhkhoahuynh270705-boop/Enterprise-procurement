package com.example.shopping.integration.mail.service;

import com.example.shopping.integration.kafka.model.ProcurementEvent;
import com.example.shopping.integration.mail.enums.NotificationChannel;

public interface NotificationService {

    NotificationChannel getChannel();

    void sendNotification(ProcurementEvent event);
}
