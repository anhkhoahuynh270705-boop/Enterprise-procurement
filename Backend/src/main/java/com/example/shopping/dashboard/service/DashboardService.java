package com.example.shopping.dashboard.service;

import com.example.shopping.dashboard.dto.response.DashboardReportDto;
import java.time.YearMonth;
import java.util.*;

import com.example.shopping.procurement.enums.ProcurementStatus;

public interface DashboardService {
    DashboardReportDto report(String supplier, String product, String department, ProcurementStatus status, YearMonth month, String currency);
}
