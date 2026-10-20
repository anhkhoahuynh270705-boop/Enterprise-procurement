package com.example.shopping.procurement.service.impl;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.procurement.mapper.ProcurementMapper;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;
import com.example.shopping.security.TicketAccess;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProcurementTicketCache {

    private static final String CACHE_NAME = "procurement_tickets";

    ProcurementTicketRepository ticketRepository;
    ProcurementMapper procurementMapper;

    @Cacheable(
            cacheNames = CACHE_NAME,
            key = "#id.toString() + ':' + T(com.example.shopping.security.TicketAccess).cacheScope()")
    @Transactional(readOnly = true)
    public ProcurementTicketResponseDto getReadableTicket(UUID id) {
        var ticket = ticketRepository.findByIdWithItems(id)
                .orElseThrow(() -> ErrorEnum.TICKET_NOT_FOUND.ex());

        TicketAccess.requireRead(ticket);
        ProcurementTicketResponseDto response = procurementMapper.toDto(ticket);
        response.setCamundaTaskId(null);
        return response;
    }
}
