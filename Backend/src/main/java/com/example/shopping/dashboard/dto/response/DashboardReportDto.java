package com.example.shopping.dashboard.dto.response;

import java.math.BigDecimal;
import java.util.List;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;

public record DashboardReportDto(
    long totalUsers, 
    long activeUsers, 
    int totalTickets, 
    long pendingTickets,
    long approvedTickets, 
    long rejectedTickets, 
    BigDecimal totalProcurementAmount,
    long approvalRate, 
    List<Long> statusCounts, 
    List<DashboardPointDto> departmentSummary,
    List<DashboardPointDto> monthlySummary, 
    List<DashboardPointDto> supplierSummary,
    List<ProcurementTicketResponseDto> recentTickets, 
    List<String> currencies
) { }
