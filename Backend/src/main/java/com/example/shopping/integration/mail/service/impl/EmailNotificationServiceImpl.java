package com.example.shopping.integration.mail.service.impl;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.shopping.integration.kafka.model.ProcurementEvent;
import com.example.shopping.integration.kafka.event.enums.ProcurementEventType;
import com.example.shopping.integration.mail.enums.NotificationChannel;
import com.example.shopping.integration.mail.service.EmailNotificationService;
import com.example.shopping.integration.mail.service.MailService;
import com.example.shopping.user.entity.UserEntity;
import com.example.shopping.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationServiceImpl implements EmailNotificationService {
        
    /* Determine the recipient: When approved/rejected, send to Maker */
    private final MailService mailService;
    private final UserRepository userRepository;
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void sendNotification(ProcurementEvent event) {
        if (event == null) {
            return;
        }
        String targetUsername;
        if (event.getEventType() == ProcurementEventType.TICKET_APPROVED 
                || event.getEventType() == ProcurementEventType.TICKET_REJECTED) {
            targetUsername = event.getMakerUsername();
        } else {
            targetUsername = event.getMakerUsername() != null ? event.getMakerUsername() : event.getCheckerUsername();
        }

        String recipientEmail = resolveEmail(targetUsername);
        String subject = buildSubject(event);
        String htmlContent = buildHtmlContent(event, targetUsername);

        log.info("Gửi email thông báo sự kiện:",
                event.getEventType(), event.getTicketCode(), targetUsername, recipientEmail);

        mailService.sendHtmlMail(recipientEmail, subject, htmlContent);
    }

    private String resolveEmail(String username) {
        if (username == null || username.isBlank()) {
            return "system@example.com";
        }
        if (username.contains("@")) {
            return username;
        }
        return userRepository.findByUsername(username)
                .map(UserEntity::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .orElse(username + "@company.com");
    }

    private String buildSubject(ProcurementEvent event) {
        String statusText = switch (event.getEventType()) {
            case TICKET_APPROVED -> "ĐÃ ĐƯỢC PHÊ DUYỆT";
            case TICKET_REJECTED -> "ĐÃ BỊ TỪ CHỐI";
            case TICKET_SUBMITTED -> "ĐÃ NỘP CHỜ DUYỆT";
            case TICKET_CREATED -> "ĐÃ TẠO MỚI";
        };
        return String.format("[Enterprise Procurement] Phiếu %s - %s", event.getTicketCode(), statusText);
    }

    private String buildHtmlContent(ProcurementEvent event, String recipientName) {
        String badgeColor;
        String badgeText;
        String actionDescription;

        switch (event.getEventType()) {
            case TICKET_APPROVED -> {
                badgeColor = "#16a34a";
                badgeText = "ĐÃ PHÊ DUYỆT";
                actionDescription = "Phiếu mua sắm của bạn đã được Checker phê duyệt thành công.";
            }
            case TICKET_REJECTED -> {
                badgeColor = "#dc2626";
                badgeText = "ĐÃ BỊ TỪ CHỐI";
                actionDescription = "Phiếu mua sắm của bạn đã bị từ chối phê duyệt. Vui lòng xem lý do bên dưới.";
            }
            case TICKET_SUBMITTED -> {
                badgeColor = "#2563eb";
                badgeText = "ĐANG CHỜ DUYỆT";
                actionDescription = "Phiếu mua sắm đã được nộp lên cấp phê duyệt.";
            }
            default -> {
                badgeColor = "#4b5563";
                badgeText = "ĐÃ KHỞI TẠO";
                actionDescription = "Phiếu mua sắm vừa được tạo trong hệ thống.";
            }
        }

        String formattedAmount = "0";
        if (event.getTotalAmount() != null) {
            NumberFormat nf = NumberFormat.getInstance(Locale.GERMANY);
            formattedAmount = nf.format(event.getTotalAmount());
        }

        String timeStr = event.getTimestamp() != null ? event.getTimestamp().format(DATE_FORMATTER) : "N/A";

        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("badgeColor", badgeColor);
        context.setVariable("badgeText", badgeText);
        context.setVariable("actionDescription", actionDescription);
        context.setVariable("ticketCode", event.getTicketCode() != null ? event.getTicketCode() : "N/A");
        context.setVariable("title", event.getTitle() != null ? event.getTitle() : "N/A");
        context.setVariable("totalAmount", formattedAmount);
        context.setVariable("currency", event.getCurrency() != null ? event.getCurrency() : "VND");
        context.setVariable("makerUsername", event.getMakerUsername() != null ? event.getMakerUsername() : "N/A");
        context.setVariable("checkerUsername", event.getCheckerUsername());
        context.setVariable("comment", event.getComment());
        context.setVariable("timestamp", timeStr);

        return templateEngine.process("mail/procurement-notification", context);
    }
}
