package com.example.shopping.procurement.service.impl;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.integration.kafka.event.ProcurementEventPublisher;
import com.example.shopping.integration.kafka.event.enums.ProcurementEventType;
import com.example.shopping.integration.kafka.model.ProcurementEvent;
import com.example.shopping.procurement.dto.request.CreateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.request.ProcurementItemRequestDto;
import com.example.shopping.procurement.dto.request.UpdateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.procurement.entity.ProcurementItem;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.mapper.ProcurementMapper;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;
import com.example.shopping.procurement.service.ProcurementService;
import com.example.shopping.procurement.service.ProcurementSkuGenerator;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.workflow.camunda.service.WorkflowTaskService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProcurementServiceImpl implements ProcurementService {

    ProcurementTicketRepository ticketRepository;
    ProcurementMapper procurementMapper;
    WorkflowTaskService workflowTaskService;
    ProcurementEventPublisher eventPublisher;
    ProcurementSkuGenerator skuGenerator;

    /* Sinh mã phiếu mua sắm tự động theo năm */
    private synchronized String generateNextTicketCode() {
        int currentYear = Year.now().getValue();
        String prefix = "PR-" + currentYear + "-";
        long count = ticketRepository.findMaxSequenceByTicketCodePrefix(prefix) + 1;
        return String.format("PR-%d-%04d", currentYear, count);
    }

    @Override
    public void recalculate(ProcurementTicket ticket) {
        BigDecimal total = BigDecimal.ZERO;
        if (ticket.getItems() != null) {
            for (var item : ticket.getItems()) {
                BigDecimal price = (item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice())
                        .multiply(BigDecimal.valueOf(item.getQuantity() == null ? 1 : item.getQuantity()));
                item.setTotalPrice(price);
                total = total.add(price);
            }
        }
        ticket.setTotalAmount(total);
    }

    /* Tạo mới phiếu mua sắm */
    @Override
    @Transactional
    public ProcurementTicketResponseDto createTicket(CreateProcurementTicketRequestDto request, String makerUsername) {
        String ticketCode = generateNextTicketCode();
        ProcurementTicket ticket = procurementMapper.toEntity(request, ticketCode, makerUsername);
        ticket.getItems().forEach(item -> item.setItemCode(skuGenerator.generateNextSku()));
        ticket.setStatus(request.isSubmitImmediately() ? ProcurementStatus.PENDING_APPROVAL : ProcurementStatus.DRAFT);
        recalculate(ticket);
        ProcurementTicket saved = ticketRepository.save(ticket);

        log.info("Đã tạo phiếu mua sắm mới:", ticketCode, makerUsername);

        eventPublisher.publishEvent(ProcurementEvent.builder()
                 .ticketId(saved.getId())
                 .ticketCode(saved.getTicketCode())
                 .title(saved.getTitle())
                 .makerUsername(makerUsername)
                 .totalAmount(saved.getTotalAmount())
                 .currency(saved.getCurrency())
                 .eventType(ProcurementEventType.TICKET_CREATED)
                 .build());

        if (request.isSubmitImmediately()) {
            workflowTaskService.startProcurementProcess(saved);
        }

        return toResponseDtoWithTaskId(saved);
    }

    /* Nộp phiếu mua sắm để Checker duyệt */
    @Override
    @Transactional
    @CacheEvict(value = "procurement_tickets", key = "#id")
    public ProcurementTicketResponseDto submitTicket(UUID id, String makerUsername) {
        ProcurementTicket ticket = ticketRepository.findByIdWithItems(id)
                .orElseThrow(() -> ErrorEnum.TICKET_NOT_FOUND.ex());

        TicketAccess.requireSubmit(ticket);
        if (ticket.getStatus() != ProcurementStatus.DRAFT && ticket.getStatus() != ProcurementStatus.REJECTED) {
            throw ErrorEnum.TICKET_CANNOT_SUBMIT.ex();
        }

        workflowTaskService.startProcurementProcess(ticket);

        eventPublisher.publishEvent(ProcurementEvent.builder()
                .ticketId(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .title(ticket.getTitle())
                .makerUsername(ticket.getMakerUsername())
                .totalAmount(ticket.getTotalAmount())
                .currency(ticket.getCurrency())
                .eventType(ProcurementEventType.TICKET_SUBMITTED)
                .build());

        return toResponseDtoWithTaskId(ticket);
    }

    /* Lấy chi tiết phiếu mua sắm */
    @Override
    @Transactional(readOnly = true)
    public ProcurementTicketResponseDto getTicketById(UUID id) {
        ProcurementTicket ticket = getTicketEntity(id);
        return toResponseDtoWithTaskId(ticket);
    }

    /* Lấy entity phiếu trực tiếp */
    @Override
    @Transactional(readOnly = true)
    public ProcurementTicket getTicketEntity(UUID id) {
        ProcurementTicket ticket = ticketRepository.findByIdWithItems(id)
                .orElseThrow(() -> ErrorEnum.TICKET_NOT_FOUND.ex());
        TicketAccess.requireRead(ticket);
        return ticket;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcurementTicketResponseDto> getAllTickets(ProcurementStatus status, String maker) {
        TicketAccess.requireReader();
        if (!TicketAccess.hasRole("ADMIN")) {
            if (TicketAccess.hasRole("CHECKER")) {
                status = ProcurementStatus.PENDING_APPROVAL;
                maker = null;
            } else {
                maker = null;
            }
        }
        List<ProcurementTicket> tickets = ticketRepository.findVisibleTickets(status, maker == null || maker.isBlank() ? null : maker);

        return tickets.stream()
                .map(this::toResponseDtoWithTaskId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcurementTicketResponseDto> getTicketPage(ProcurementStatus status, String maker, Pageable pageable) {
        TicketAccess.requireReader();
        if (!TicketAccess.hasRole("ADMIN")) {
            if (TicketAccess.hasRole("CHECKER")) {
                status = ProcurementStatus.PENDING_APPROVAL;
                maker = null;
            } else {
                maker = null;
            }
        }
        Page<ProcurementTicket> tickets = ticketRepository.findVisibleTickets(status, maker == null || maker.isBlank() ? null : maker, pageable);

        return tickets.map(procurementMapper::toDto);
    }

    @Override
    @Transactional
    @CacheEvict(value = "procurement_tickets", key = "#id")
    public ProcurementTicketResponseDto updateTicket(UUID id, UpdateProcurementTicketRequestDto request, String username) {
        ProcurementTicket ticket = ticketRepository.findByIdWithItems(id)
                .orElseThrow(() -> ErrorEnum.TICKET_NOT_FOUND.ex());

        TicketAccess.requireSubmit(ticket);
        if (ticket.getStatus() != ProcurementStatus.DRAFT && ticket.getStatus() != ProcurementStatus.REJECTED
                && !(TicketAccess.hasRole("ADMIN") && ticket.getStatus() == ProcurementStatus.APPROVED)) {
            throw ErrorEnum.TICKET_CANNOT_EDIT.ex();
        }

        ticket.setTitle(request.getTitle());
        ticket.setDepartment(request.getDepartment());
        ticket.setReason(request.getReason());
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getItems() != null) {
            Set<String> existingSkus = ticket.getItems().stream()
                    .map(ProcurementItem::getItemCode)
                    .filter(itemCode -> itemCode != null && !itemCode.isBlank())
                    .collect(Collectors.toSet());
            ticket.getItems().clear();
            for (ProcurementItemRequestDto itemDto : request.getItems()) {
                ProcurementItem item = procurementMapper.toItemEntity(itemDto);
                String requestedSku = itemDto.getItemCode();
                if (requestedSku == null || !existingSkus.remove(requestedSku)) {
                    item.setItemCode(skuGenerator.generateNextSku());
                } else {
                    item.setItemCode(requestedSku);
                }
                ticket.getItems().add(item);
                item.setTicket(ticket);
            }
            recalculate(ticket);
        }
        ProcurementTicket updated = ticketRepository.save(ticket);
        return toResponseDtoWithTaskId(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "procurement_tickets", key = "#id")
    public void deleteTicket(UUID id, String username) {
        ProcurementTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> ErrorEnum.TICKET_NOT_FOUND.ex());

        TicketAccess.requireSubmit(ticket);
        if (ticket.getStatus() != ProcurementStatus.DRAFT && ticket.getStatus() != ProcurementStatus.REJECTED) {
            throw ErrorEnum.TICKET_CANNOT_DELETE.ex();
        }

        ticketRepository.delete(ticket);
        log.info("Đã xóa phiếu mua sắm: {}", ticket.getTicketCode());
    }

    /**
     * Helper gắn active taskId vào DTO
     */
    private ProcurementTicketResponseDto toResponseDtoWithTaskId(ProcurementTicket ticket) {
        ProcurementTicketResponseDto dto = procurementMapper.toDto(ticket);
        if (ticket.getCamundaProcessInstanceId() != null && ticket.getStatus() == ProcurementStatus.PENDING_APPROVAL) {
            String activeTaskId = workflowTaskService.getActiveTaskIdForTicket(ticket.getCamundaProcessInstanceId());
            dto.setCamundaTaskId(activeTaskId);
        }
        return dto;
    }
}
