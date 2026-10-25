package com.example.shopping.requestForQuotation.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.shopping.integration.mail.service.MailService;
import com.example.shopping.requestForQuotation.service.SupplierInvitationEmailService;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults (level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class SupplierInvitationEmailServiceImpl implements SupplierInvitationEmailService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));

    MailService mailService;
    TemplateEngine templateEngine;

    @Override
    public void sendInvitation(String recipientEmail, String supplierName, String rfqCode,
                               String invitationLink, LocalDateTime expiresAt) {
        Context context = new Context(Locale.forLanguageTag("vi-VN"));
        context.setVariable("supplierName", supplierName);
        context.setVariable("rfqCode", rfqCode);
        context.setVariable("invitationLink", invitationLink);
        context.setVariable("expiresAt", expiresAt.format(DATE_FORMAT));

        String html = templateEngine.process("mail/rfq-invitation", context);
        mailService.sendHtmlMail(
                recipientEmail,
                "Mời báo giá " + rfqCode,
                html
        );
    }
}
