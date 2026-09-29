package com.example.shopping.dashboard.service.impl;

import com.example.shopping.dashboard.service.DashboardService;

import com.example.shopping.dashboard.dto.response.DashboardPointDto;
import com.example.shopping.dashboard.dto.response.DashboardReportDto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.example.shopping.procurement.entity.*;
import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;
import com.example.shopping.procurement.mapper.ProcurementMapper;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final ProcurementTicketRepository tickets;
    private final UserRepository users;
    private final ProcurementMapper mapper;


    @Transactional(readOnly = true)
    public DashboardReportDto report(String supplier, String product, String department,
            ProcurementStatus status, YearMonth month, String currency) {
        List<ProcurementTicket> source = tickets.findAllForDashboard();
        List<String> currencies = source
                                .stream().map(ProcurementTicket::getCurrency)
                                .filter(Objects::nonNull)
                                .distinct()
                                .sorted()
                                .toList();
        List<ProcurementTicketResponseDto> matching = new ArrayList<>();
        Map<String, BigDecimal> departments = new TreeMap<>();
        Map<String, BigDecimal> months = new TreeMap<>();
        Map<String, BigDecimal> suppliers = new TreeMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ProcurementTicket ticket : source) {
            if (!matches(currency, ticket.getCurrency()) || !matches(department, label(ticket.getDepartment()))
                    || (status != null && status != ticket.getStatus())
                    || (month != null && (ticket.getCreatedAt() == null
                        || !month.equals(YearMonth.from(ticket.getCreatedAt()))))) 
                        continue;
            List<ProcurementItem> items = ticket.getItems()
                                        .stream()
                                        .filter(item -> matches(supplier, label(item.getSupplierName())) && matches(product, productKey(item)))
                                        .toList();
            boolean itemFiltered = present(supplier) || present(product);
            if (itemFiltered && items.isEmpty()) continue;
            BigDecimal amount = itemFiltered
                    ? items.stream().map(DashboardServiceImpl::amount).reduce(BigDecimal.ZERO, BigDecimal::add)
                    : zero(ticket.getTotalAmount());
            var dto = mapper.toDto(ticket);
            dto.setItems(mapper.toItemDtos(items));
            dto.setTotalAmount(amount);
            matching.add(dto);
            total = total.add(amount);
            departments.merge(label(ticket.getDepartment()), BigDecimal.ONE, BigDecimal::add);
            if (ticket.getCreatedAt() != null)
                months.merge(YearMonth.from(ticket.getCreatedAt()).toString(), amount, BigDecimal::add);
            for (var item : items)
                suppliers.merge(label(item.getSupplierName()), amount(item), BigDecimal::add);
        }
        List<ProcurementStatus> order = List.of(ProcurementStatus.APPROVED, ProcurementStatus.PENDING_APPROVAL,
                ProcurementStatus.REJECTED, ProcurementStatus.DRAFT, ProcurementStatus.CANCELLED);
        List<Long> counts = order.stream()
                .map(s -> matching.stream().filter(t -> t.getStatus() == s).count()).toList();
        matching.sort(Comparator.comparing(ProcurementTicketResponseDto::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return new DashboardReportDto(users.count(), users.countByEnabledTrue(), matching.size(), counts.get(1),
                counts.get(0), counts.get(2), total,
                matching.isEmpty() ? 0 : Math.round(counts.get(0) * 100.0 / matching.size()), counts,
                points(departments), points(months), points(suppliers),
                matching.stream().limit(50).toList(), currencies);
    }

    private static List<DashboardPointDto> points(Map<String, BigDecimal> values) {
        return values.entrySet().stream().map(e -> new DashboardPointDto(e.getKey(), e.getKey(), e.getValue())).toList();
    }

    private static boolean present(String value) { 
        return value != null && !value.isBlank(); 
}

    private static boolean matches(String filter, String value) {
        return !present(filter) || filter.trim().equals(value == null ? "" : value.trim());
    }
    private static String label(String value) { 
        return present(value) ? value.trim() : "Chưa xác định"; 
}

    private static String productKey(ProcurementItem item) {
        return present(item.getItemCode()) ? "code:" + item.getItemCode().trim() : "name:" + item.getItemName().trim();
    }

    private static BigDecimal zero(BigDecimal value) { 
        return value == null ? BigDecimal.ZERO : value; 
}

    private static BigDecimal amount(ProcurementItem item) {
        return item.getTotalPrice() != null ? item.getTotalPrice()
                : zero(item.getUnitPrice()).multiply(BigDecimal.valueOf(item.getQuantity() == null ? 0 : item.getQuantity()));
    }
}
