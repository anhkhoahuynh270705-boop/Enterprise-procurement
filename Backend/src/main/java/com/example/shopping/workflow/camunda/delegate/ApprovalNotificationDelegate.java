package com.example.shopping.workflow.camunda.delegate;

import java.time.LocalDateTime;
import java.util.UUID;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.integration.kafka.event.ProcurementEventPublisher;
import com.example.shopping.integration.kafka.model.ProcurementEvent;
import com.example.shopping.integration.kafka.event.enums.ProcurementEventType;
import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("approvalNotificationDelegate")
@RequiredArgsConstructor
public class ApprovalNotificationDelegate implements JavaDelegate {

    private final ProcurementTicketRepository ticketRepository;
    private final ProcurementEventPublisher eventPublisher;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        String ticketIdStr = (String) execution.getVariable("ticketId");
        String checkerUsername = (String) execution.getVariable("checkerUsername");
        String checkerComment = (String) execution.getVariable("checkerComment");

        log.info("Xử lý hậu phê duyệt cho phiếu ID:", ticketIdStr, checkerUsername);

        if (ticketIdStr != null) {
            UUID ticketId = UUID.fromString(ticketIdStr);
            ProcurementTicket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket != null) {
                ticket.setStatus(ProcurementStatus.APPROVED);
                ticket.setCheckerUsername(checkerUsername);
                ticket.setCheckerComment(checkerComment);
                ticket.setApprovedAt(LocalDateTime.now());
                ticketRepository.save(ticket);

                // Evict cache
                if (cacheManager.getCache("procurement_tickets") != null) {
                    cacheManager.getCache("procurement_tickets").evict(ticketId);
                }

                // Phát hành sự kiện vào Message Queue / Kafka
                ProcurementEvent event = ProcurementEvent.builder()
                        .ticketId(ticket.getId())
                        .ticketCode(ticket.getTicketCode())
                        .title(ticket.getTitle())
                        .makerUsername(ticket.getMakerUsername())
                        .checkerUsername(checkerUsername)
                        .totalAmount(ticket.getTotalAmount())
                        .currency(ticket.getCurrency())
                        .eventType(ProcurementEventType.TICKET_APPROVED)
                        .comment(checkerComment)
                        .build();

                eventPublisher.publishEvent(event);
            }
        }
    }
}
