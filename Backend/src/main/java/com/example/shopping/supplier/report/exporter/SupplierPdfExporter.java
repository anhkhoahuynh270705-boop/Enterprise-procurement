package com.example.shopping.supplier.report.exporter;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.shopping.common.report.service.JasperReportService;
import com.example.shopping.supplier.entity.SupplierEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplierPdfExporter {

    private final JasperReportService jasperReportService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String getContentType() {
        return "application/pdf";
    }

    public String getFileExtension() {
        return "pdf";
    }

    /**
     * Xuất hồ sơ chi tiết 1 nhà cung cấp ra PDF
     */
    public byte[] exportSupplier(SupplierEntity supplier) throws Exception {
        log.info("Đang xuất PDF hồ sơ nhà cung cấp:", supplier.getCode(), supplier.getName());

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("CODE", supplier.getCode() != null ? supplier.getCode() : "");
        parameters.put("NAME", supplier.getName() != null ? supplier.getName() : "");
        parameters.put("CONTACT_PERSON", supplier.getContactPerson() != null ? supplier.getContactPerson() : "—");
        parameters.put("EMAIL", supplier.getEmail() != null ? supplier.getEmail() : "—");
        parameters.put("PHONE", supplier.getPhone() != null ? supplier.getPhone() : "—");
        parameters.put("ADDRESS", supplier.getAddress() != null ? supplier.getAddress() : "");
        parameters.put("CITY", supplier.getCity() != null ? supplier.getCity() : "");
        parameters.put("COUNTRY", supplier.getCountry() != null ? supplier.getCountry() : "");
        parameters.put("TAX_CODE", supplier.getTaxCode() != null ? supplier.getTaxCode() : "—");
        parameters.put("BANK_NAME", supplier.getBankName() != null ? supplier.getBankName() : "—");
        parameters.put("BANK_ACCOUNT", supplier.getBankAccount() != null ? supplier.getBankAccount() : "—");
        parameters.put("STATUS", supplier.getStatus() != null ? supplier.getStatus().name() : "ACTIVE");
        parameters.put("CREATED_AT", supplier.getCreatedAt() != null ? supplier.getCreatedAt().format(dateFormatter) : "—");
        parameters.put("NOTES", supplier.getNotes() != null ? supplier.getNotes() : "");

        return jasperReportService.generatePdf("/reports/supplier-profile.jrxml", parameters, null);
    }
}
