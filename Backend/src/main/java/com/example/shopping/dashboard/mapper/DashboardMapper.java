package com.example.shopping.dashboard.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.mapstruct.Mapper;

import com.example.shopping.dashboard.dto.response.DashboardPointDto;
import com.example.shopping.dashboard.dto.response.DashboardReportDto;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;

@Mapper(componentModel = "spring")
public interface DashboardMapper {

    default DashboardPointDto toPoint(String key, String label, BigDecimal value) {
        return new DashboardPointDto(key, label, value);
    }

    default List<DashboardPointDto> toPoints(Map<String, BigDecimal> values) {
        if (values == null) {
            return List.of();
        }
        return values.entrySet().stream()
                .map(e -> new DashboardPointDto(e.getKey(), e.getKey(), e.getValue()))
                .toList();
    }

    default DashboardReportDto toReportDto(
            long totalUsers,
            long activeUsers,
            int totalTickets,
            long pendingTickets,
            long approvedTickets,
            long rejectedTickets,
            BigDecimal totalProcurementAmount,
            long approvalRate,
            List<Long> statusCounts,
            Map<String, BigDecimal> departments,
            Map<String, BigDecimal> months,
            Map<String, BigDecimal> suppliers,
            List<ProcurementTicketResponseDto> recentTickets,
            List<String> currencies) {
        return new DashboardReportDto(
                totalUsers,
                activeUsers,
                totalTickets,
                pendingTickets,
                approvedTickets,
                rejectedTickets,
                totalProcurementAmount,
                approvalRate,
                statusCounts,
                toPoints(departments),
                toPoints(months),
                toPoints(suppliers),
                recentTickets,
                currencies
        );
    }
}
