package com.example.shopping.integration.mail.factory;

import com.example.shopping.integration.mail.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.shopping.integration.mail.enums.NotificationChannel;

@Component
public class NotificationServiceFactory {

    private final Map<NotificationChannel, NotificationService> services;

    public NotificationServiceFactory(List<NotificationService> serviceList) {
        this.services = serviceList.stream()
                .collect(Collectors.toMap(NotificationService::getChannel, s -> s));
    }

    public NotificationService getService(NotificationChannel channel) {
        NotificationService service = services.get(channel);
        if (service == null) {
            throw new IllegalArgumentException("Không tìm thấy NotificationService cho kênh: " + channel);
        }
        return service;
    }

    public List<NotificationService> getAllServices() {
        return List.copyOf(services.values());
    }
}
