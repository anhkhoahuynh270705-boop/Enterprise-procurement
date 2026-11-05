package com.example.shopping.workflow.camunda.delegate.procurementdelegate;

import java.util.UUID;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.kafka.event.ProcurementEventPublisher;
import com.example.shopping.procurement.kafka.event.enums.ProcurementEventType;
import com.example.shopping.procurement.kafka.model.ProcurementEvent;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;
import com.example.shopping.workflow.camunda.event.WorkflowReviewEvent;
import com.example.shopping.workflow.camunda.event.WorkflowReviewEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("rejectionNotificationDelegate")
@RequiredArgsConstructor
public class RejectionNotificationDelegate implements JavaDelegate {

    private final ProcurementTicketRepository ticketRepository;
    private final ProcurementEventPublisher eventPublisher;
    private final WorkflowReviewEventPublisher workflowReviewEventPublisher;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        String ticketIdStr = (String) 
        execution.getVariable("ticketId");
        String checkerUsername = (String) 
        execution.getVariable("checkerUsername");
        String checkerComment = (String) 
        execution.getVariable("checkerComment");

        log.info("Xử lý từ chối cho phiếu ID:", ticketIdStr, checkerUsername);

        if (ticketIdStr != null) {
            UUID ticketId = UUID.fromString(ticketIdStr);
            ProcurementTicket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket != null) {
                ticket.setStatus(ProcurementStatus.REJECTED);
                ticket.setCheckerUsername(checkerUsername);
                ticket.setCheckerComment(checkerComment);
                ticketRepository.save(ticket);

                // Evict cache
                if (cacheManager.getCache("procurement_tickets") != null) {
                    cacheManager.getCache("procurement_tickets").clear();
                }

                // Phát hành sự kiện vào Message Queue 
                ProcurementEvent event = ProcurementEvent.builder()
                        .ticketId(ticket.getId())
                        .ticketCode(ticket.getTicketCode())
                        .title(ticket.getTitle())
                        .makerUsername(ticket.getMakerUsername())
                        .checkerUsername(checkerUsername)
                        .totalAmount(ticket.getTotalAmount())
                        .currency(ticket.getCurrency())
                        .eventType(ProcurementEventType.TICKET_REJECTED)
                        .comment(checkerComment)
                        .build();

                eventPublisher.publishEvent(event);
                workflowReviewEventPublisher.publish(new WorkflowReviewEvent(
                        "PROCUREMENT_TICKET", ticket.getId(), ticket.getTicketCode(), ticket.getTitle(),
                        ticket.getMakerUsername(), checkerUsername, ticket.getStatus().name(), checkerComment,
                        ticket.getTotalAmount(), java.time.LocalDateTime.now()));
            }
        }
    }
}
