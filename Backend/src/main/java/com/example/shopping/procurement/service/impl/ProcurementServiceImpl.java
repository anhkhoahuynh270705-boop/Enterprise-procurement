package com.example.shopping.procurement.service.impl;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import com.example.shopping.security.TicketAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.integration.kafka.model.ProcurementEvent;
import com.example.shopping.integration.kafka.event.ProcurementEventPublisher;
import com.example.shopping.integration.kafka.event.enums.ProcurementEventType;
import com.example.shopping.procurement.dto.request.CreateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.request.UpdateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.request.ProcurementItemRequestDto;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.procurement.entity.ProcurementItem;
import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.mapper.ProcurementMapper;
import com.example.shopping.procurement.repository.ProcurementItemRepository;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;
import com.example.shopping.procurement.service.ProcurementService;
import com.example.shopping.procurement.service.ProcurementCalculationService;
import com.example.shopping.workflow.camunda.service.WorkflowTaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementServiceImpl implements ProcurementService {

    private final ProcurementTicketRepository ticketRepository;
    private final ProcurementItemRepository itemRepository;
    private final ProcurementMapper procurementMapper;
    private final WorkflowTaskService workflowTaskService;
    private final ProcurementEventPublisher eventPublisher;
    private final ProcurementCalculationService calculations;

    /**
     * Sinh mã phiếu mua sắm tự động theo năm
     */
    private synchronized String generateNextTicketCode() {
        int currentYear = Year.now().getValue();
        String prefix = "PR-" + currentYear + "-";
        long count = ticketRepository.findMaxSequenceByTicketCodePrefix(prefix) + 1;
        return String.format("PR-%d-%04d", currentYear, count);
    }

    /**
     * Tạo mới phiếu mua sắm
     */
    @Override
    @Transactional
    public ProcurementTicketResponseDto createTicket(CreateProcurementTicketRequestDto request, String makerUsername) {
        String ticketCode = generateNextTicketCode();
        ProcurementTicket ticket = procurementMapper.toEntity(request, ticketCode, makerUsername);
        ticket.setStatus(request.isSubmitImmediately() ? ProcurementStatus.PENDING_APPROVAL : ProcurementStatus.DRAFT);
        calculations.recalculate(ticket);
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

    /**
     * Nộp phiếu mua sắm để Checker duyệt
     */
    @Override
    @Transactional
    @CacheEvict(value = "procurement_tickets", key = "#id")
    public ProcurementTicketResponseDto submitTicket(UUID id, String makerUsername) {
        ProcurementTicket ticket = ticketRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mua sắm với ID: " + id));

        TicketAccess.requireSubmit(ticket);
        if (ticket.getStatus() != ProcurementStatus.DRAFT && ticket.getStatus() != ProcurementStatus.REJECTED) {
            throw new IllegalStateException("Chỉ phiếu ở trạng thái Nháp hoặc Đã từ chối mới được nộp duyệt.");
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

    /**
     * Lấy chi tiết phiếu mua sắm
     */
    @Override
    @Transactional(readOnly = true)
    public ProcurementTicketResponseDto getTicketById(UUID id) {
        ProcurementTicket ticket = getTicketEntity(id);
        return toResponseDtoWithTaskId(ticket);
    }

    /**
     * Lấy entity phiếu trực tiếp
     */
    @Override
    @Transactional(readOnly = true)
    public ProcurementTicket getTicketEntity(UUID id) {
        ProcurementTicket ticket = ticketRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mua sắm với ID: " + id));
        TicketAccess.requireRead(ticket);
        return ticket;
    }

    /**
     * Lấy danh sách tất cả các phiếu
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProcurementTicketResponseDto> getAllTickets(ProcurementStatus status, String maker) {
        TicketAccess.requireReader();
        if (!TicketAccess.hasRole("ADMIN")) {
            if (TicketAccess.hasRole("CHECKER")) {
                status = ProcurementStatus.PENDING_APPROVAL;
                maker = null;
            } else {
                maker = TicketAccess.username();
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
                maker = TicketAccess.username();
            }
        }
        Page<ProcurementTicket> tickets = ticketRepository.findVisibleTickets(status, maker == null || maker.isBlank() ? null : maker, pageable);

        return tickets.map(procurementMapper::toDto);
    }

    /**
     * Cập nhật thông tin phiếu
     */
    @Override
    @Transactional
    @CacheEvict(value = "procurement_tickets", key = "#id")
    public ProcurementTicketResponseDto updateTicket(UUID id, UpdateProcurementTicketRequestDto request, String username) {
        ProcurementTicket ticket = ticketRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mua sắm với ID: " + id));

        TicketAccess.requireSubmit(ticket);
        if (ticket.getStatus() != ProcurementStatus.DRAFT && ticket.getStatus() != ProcurementStatus.REJECTED
                && !(TicketAccess.hasRole("ADMIN") && ticket.getStatus() == ProcurementStatus.APPROVED)) {
            throw new IllegalStateException("Chỉ được sửa phiếu ở trạng thái Nháp hoặc bị từ chối.");
        }

        ticket.setTitle(request.getTitle());
        ticket.setDepartment(request.getDepartment());
        ticket.setReason(request.getReason());
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getItems() != null) {
            ticket.getItems().clear();
            for (ProcurementItemRequestDto itemDto : request.getItems()) {
                ProcurementItem item = procurementMapper.toItemEntity(itemDto);
                ticket.getItems().add(item);
                item.setTicket(ticket);
            }
            calculations.recalculate(ticket);
        }

        ProcurementTicket updated = ticketRepository.save(ticket);
        return toResponseDtoWithTaskId(updated);
    }

    /**
     * Xóa phiếu mua sắm
     */
    @Override
    @Transactional
    @CacheEvict(value = "procurement_tickets", key = "#id")
    public void deleteTicket(UUID id, String username) {
        ProcurementTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu mua sắm với ID: " + id));

        TicketAccess.requireSubmit(ticket);
        if (ticket.getStatus() != ProcurementStatus.DRAFT && ticket.getStatus() != ProcurementStatus.REJECTED
                && !(TicketAccess.hasRole("ADMIN") && ticket.getStatus() == ProcurementStatus.APPROVED)) {
            throw new IllegalStateException("Chỉ được xóa phiếu ở trạng thái Nháp hoặc bị từ chối.");
        }

        ticketRepository.delete(ticket);
        log.info("Đã xóa phiếu mua sắm: [{}]", ticket.getTicketCode());
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
