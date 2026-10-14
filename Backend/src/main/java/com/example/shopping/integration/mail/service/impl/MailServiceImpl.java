package com.example.shopping.integration.mail.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.shopping.integration.mail.service.MailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MailServiceImpl implements MailService {

    JavaMailSender javaMailSender;

    @Value("${spring.mail.from:huynhanhkhoa2707@gmail.com}")
    @NonFinal
    private String fromEmail;

    public MailServiceImpl(@Autowired(required = false) JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendMail(String to, String subject, String content) {
        sendEmailInternal(to, subject, content, false);
    }

    @Override
    public void sendHtmlMail(String to, String subject, String htmlContent) {
        sendEmailInternal(to, subject, htmlContent, true);
    }

    private void sendEmailInternal(String to, String subject, String content, boolean isHtml) {
        log.info("Chuẩn bị gửi email tới:", to, subject, isHtml);

        if (javaMailSender == null) {
            log.warn("JavaMailSender chưa được cấu hình.", content);
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            javaMailSender.send(message);
            log.info("Đã gửi email thành công tới: ", to);
        } catch (MessagingException e) {
            log.error("Lỗi khi gửi email tới: ", to, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Lỗi không xác định khi gửi email tới: ", to, e.getMessage(), e);
        }
    }
}
