package com.example.shopping.workflow.camunda.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import com.example.shopping.integration.mail.service.MailService;
import com.example.shopping.user.entity.UserEntity;
import com.example.shopping.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowReviewEventListener {

    private final MailService mailService;
    private final UserRepository userRepository;

    @KafkaListener(
            topics = WorkflowReviewEventPublisher.TOPIC_WORKFLOW_REVIEW_EVENTS,
            groupId = "workflow-review-notification-group")
    public void onReviewCompleted(WorkflowReviewEvent event) {
        if (event == null || event.submittedBy() == null || event.submittedBy().isBlank()) return;

        // Procurement already sends its established notification event on procurement-events.
        if ("PROCUREMENT_TICKET".equals(event.entityType())) return;

        String recipient = userRepository.findByUsername(event.submittedBy())
                .map(UserEntity::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .orElse(null);
        if (recipient == null) {
            log.warn("Bỏ qua email kết quả duyệt {} vì không tìm thấy email của user {}",
                    event.referenceCode(), event.submittedBy());
            return;
        }

        Boolean approved = "APPROVED".equals(event.outcome()) || "SENT".equals(event.outcome());
        String result = approved ? "ĐÃ ĐƯỢC PHÊ DUYỆT" : "ĐÃ BỊ TỪ CHỐI";
        String subject = "[Enterprise Procurement] " + event.referenceCode() + " - " + result;
        String html = "<div style=\"font-family:Arial,sans-serif;color:#1f2937;line-height:1.6\">"
                + "<h2>" + result + "</h2>"
                + "<p>Hồ sơ <strong>" + escape(event.referenceCode()) + "</strong> đã được xử lý.</p>"
                + "<p><strong>Loại hồ sơ:</strong> " + escape(event.entityType()) + "<br>"
                + "<strong>Tiêu đề:</strong> " + escape(event.title()) + "<br>"
                + "<strong>Kết quả:</strong> " + escape(event.outcome()) + "<br>"
                + "<strong>Người duyệt:</strong> " + escape(event.reviewedBy()) + "</p>"
                + (event.comment() == null || event.comment().isBlank()
                        ? "" : "<p><strong>Nhận xét:</strong> " + escape(event.comment()) + "</p>")
                + "</div>";
        try {
            mailService.sendHtmlMail(recipient, subject, html);
        } catch (Exception exception) {
            log.error("Không gửi được email kết quả duyệt {} tới {}",
                    event.referenceCode(), recipient, exception);
        }
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "—" : value);
    }
}
