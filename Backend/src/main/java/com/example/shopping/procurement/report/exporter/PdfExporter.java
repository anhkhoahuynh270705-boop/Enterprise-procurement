package com.example.shopping.procurement.report.exporter;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.shopping.procurement.dto.response.ProcurementItemDto;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.mapper.ProcurementMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.shopping.user.repository.UserRepository;
import com.example.shopping.user.entity.UserEntity;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfExporter {

    private final UserRepository users;

    private final ProcurementMapper procurementMapper;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0");

    public String getFormat() {
        return "PDF";
    }

    public String getContentType() {
        return "application/pdf";
    }

    public String getFileExtension() {
        return "pdf";
    }

    public byte[] exportTicket(ProcurementTicket ticket) throws Exception {
        log.info("Đang xuất PDF phiếu mua sắm:", ticket.getTicketCode());

        InputStream reportStream = getClass().getResourceAsStream("/reports/procurement-voucher.jrxml");
        if (reportStream == null) {
            reportStream = getClass().getResourceAsStream("/reports/procurement_ticket.jrxml");
        }
        if (reportStream == null) {
            throw new IllegalStateException("Không tìm thấy template JasperReports:");
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("TICKET_CODE", ticket.getTicketCode());
        parameters.put("TITLE", ticket.getTitle() != null ? ticket.getTitle() : "");
        parameters.put("DEPARTMENT", ticket.getDepartment() != null ? ticket.getDepartment() : "");
        parameters.put("REASON", ticket.getReason() != null ? ticket.getReason() : "");
        parameters.put("STATUS", ticket.getStatus() != null ? ticket.getStatus().name() : "");
        parameters.put("PRIORITY", ticket.getPriority() != null ? ticket.getPriority().name() : "");
        parameters.put("MAKER", fullName(ticket.getMakerUsername(), "Chưa cập nhật họ tên"));
        parameters.put("MAKER_USERNAME", fullName(ticket.getMakerUsername(), "Chưa cập nhật họ tên"));
        parameters.put("CHECKER", fullName(ticket.getCheckerUsername(), "Chưa phê duyệt"));
        parameters.put("CHECKER_USERNAME", fullName(ticket.getCheckerUsername(), "Chưa phê duyệt"));
        parameters.put("CREATED_AT", ticket.getCreatedAt() != null ? ticket.getCreatedAt().format(dateFormatter) : "");
        parameters.put("APPROVED_AT", ticket.getApprovedAt() != null ? ticket.getApprovedAt().format(dateFormatter) : "");
        parameters.put("CURRENCY", ticket.getCurrency() != null ? ticket.getCurrency() : "VND");
        parameters.put("TOTAL_AMOUNT", ticket.getTotalAmount() != null ? currencyFormat.format(ticket.getTotalAmount()) : "0.");

        List<ProcurementItemDto> itemDtos = ticket.getItems() != null
                ? ticket.getItems().stream().map(procurementMapper::toItemDto).toList()
                : List.of();
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itemDtos);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    /**
     * Xuất PDF chi tiết Enterprise Procurement Detail
     */
    public byte[] exportTicketDetail(ProcurementTicket ticket) throws Exception {
        log.info("Đang xuất PDF chi tiết Enterprise Procurement Detail:", ticket.getTicketCode());

        InputStream reportStream = getClass().getResourceAsStream("/reports/enterprise-procurement-detail.jrxml");
        if (reportStream == null) {
            reportStream = getClass().getResourceAsStream("/reports/procurement-voucher.jrxml");
        }
        if (reportStream == null) {
            throw new IllegalStateException("Không tìm thấy template JasperReports cho Enterprise Procurement Detail");
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("TICKET_CODE", ticket.getTicketCode());
        parameters.put("TITLE", ticket.getTitle() != null ? ticket.getTitle() : "");
        parameters.put("DEPARTMENT", ticket.getDepartment() != null ? ticket.getDepartment() : "");
        parameters.put("REASON", ticket.getReason() != null ? ticket.getReason() : "");
        parameters.put("STATUS", ticket.getStatus() != null ? ticket.getStatus().name() : "");
        parameters.put("PRIORITY", ticket.getPriority() != null ? ticket.getPriority().name() : "");
        parameters.put("MAKER", fullName(ticket.getMakerUsername(), "Chưa cập nhật họ tên"));
        parameters.put("CHECKER", fullName(ticket.getCheckerUsername(), "Chưa phê duyệt"));
        parameters.put("CHECKER_COMMENT", ticket.getCheckerComment() != null ? ticket.getCheckerComment() : "");
        parameters.put("PROCESS_INSTANCE_ID", ticket.getCamundaProcessInstanceId() != null ? ticket.getCamundaProcessInstanceId() : "");
        parameters.put("CREATED_AT", ticket.getCreatedAt() != null ? ticket.getCreatedAt().format(dateFormatter) : "");
        parameters.put("APPROVED_AT", ticket.getApprovedAt() != null ? ticket.getApprovedAt().format(dateFormatter) : "Chưa phê duyệt");
        parameters.put("CURRENCY", ticket.getCurrency() != null ? ticket.getCurrency() : "VND");
        parameters.put("TOTAL_AMOUNT", ticket.getTotalAmount() != null
                ? currencyFormat.format(ticket.getTotalAmount()) + " " + (ticket.getCurrency() != null ? ticket.getCurrency() : "VND")
                : "0 VND");

        List<ProcurementItemDto> itemDtos = ticket.getItems() != null
                ? ticket.getItems().stream().map(procurementMapper::toItemDto).toList()
                : List.of();
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(itemDtos);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
    private String fullName(String username, String unassignedLabel) {
        if (username == null || username.isBlank()) return unassignedLabel;
        return users.findByUsername(username)
                .map(UserEntity::getFullName)
                .filter(name -> !name.isBlank())
                .map(String::trim)
                .orElse("Chưa cập nhật họ tên");
    }
}
