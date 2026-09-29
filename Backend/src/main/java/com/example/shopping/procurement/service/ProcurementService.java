package com.example.shopping.procurement.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.shopping.procurement.dto.request.CreateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.request.UpdateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.enums.ProcurementStatus;

public interface ProcurementService {

    ProcurementTicketResponseDto createTicket(CreateProcurementTicketRequestDto request, String makerUsername);

    ProcurementTicketResponseDto submitTicket(UUID id, String makerUsername);

    ProcurementTicketResponseDto getTicketById(UUID id);

    ProcurementTicket getTicketEntity(UUID id);

    List<ProcurementTicketResponseDto> getAllTickets(ProcurementStatus status, String maker);

    Page<ProcurementTicketResponseDto> getTicketPage(ProcurementStatus status, String maker, Pageable pageable);

    ProcurementTicketResponseDto updateTicket(UUID id, UpdateProcurementTicketRequestDto request, String username);

    void deleteTicket(UUID id, String username);
}
