package com.example.shopping.dashboard.controller;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.dashboard.dto.response.DashboardReportDto;
import com.example.shopping.dashboard.service.DashboardService;
import com.example.shopping.procurement.enums.ProcurementStatus;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {
    DashboardService service;

    @GetMapping
    public DashboardReportDto report(
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String product,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) ProcurementStatus status,
            @RequestParam(required = false) String month,
            @RequestParam(defaultValue = "VND") String currency) {
        YearMonth parsedMonth = null;
        if (month != null && !month.isBlank()) {
            try { 
                parsedMonth = YearMonth.parse(month); 
            }
            catch (DateTimeParseException ex) {
                throw ErrorEnum.DASHBOARD_INVALID_MONTH_FORMAT.ex();
            }
        }
        return service.report(supplier, product, department, status, parsedMonth, currency);
    }
}
