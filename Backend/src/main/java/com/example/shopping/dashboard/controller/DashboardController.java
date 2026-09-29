package com.example.shopping.dashboard.controller;

import com.example.shopping.dashboard.service.DashboardService;
import com.example.shopping.dashboard.dto.response.DashboardReportDto;

import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import com.example.shopping.procurement.enums.ProcurementStatus;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;

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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must use YYYY-MM");
            }
        }
        return service.report(supplier, product, department, status, parsedMonth, currency);
    }
}
