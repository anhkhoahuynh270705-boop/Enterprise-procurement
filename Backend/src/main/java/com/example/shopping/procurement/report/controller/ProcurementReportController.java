package com.example.shopping.procurement.report.controller;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.shopping.common.dto.response.ApiResponse;

import com.example.shopping.procurement.report.dto.response.ImportResultDto;
import com.example.shopping.procurement.report.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementReportController {

    private final ReportService reportService;

    private String extractUsername(Jwt jwt) {
        if (jwt == null) {
            return "system_maker";
        }
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null ? preferred : jwt.getSubject();
    }

    /**
     * Xuất PDF / Excel cho 1 phiếu mua sắm
     */
    @GetMapping("/export/{id}")
    public ResponseEntity<byte[]> exportTicket(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "PDF") String format) {
        try {
            var report = reportService.exportTicket(id, format, false);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.filename() + "\"")
                    .contentType(MediaType.parseMediaType(report.contentType()))
                    .body(report.bytes());
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi xuất báo cáo phiếu mua sắm [{}]: {}", format, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất PDF / Excel Enterprise Procurement Detail
     */
    @GetMapping("/export-detail/{id}")
    public ResponseEntity<byte[]> exportTicketDetail(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "PDF") String format) {
        try {
            var report = reportService.exportTicket(id, format, true);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.filename() + "\"")
                    .contentType(MediaType.parseMediaType(report.contentType()))
                    .body(report.bytes());
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi xuất báo cáo Enterprise Procurement Detail [{}]: {}", format, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export-all")
    public ResponseEntity<byte[]> exportAllTickets(
            @RequestParam(defaultValue = "EXCEL") String format) {
        try {
            var report = reportService.exportAllTickets();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.filename() + "\"")
                    .contentType(MediaType.parseMediaType(report.contentType()))
                    .body(report.bytes());
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi xuất tổng hợp phiếu mua sắm: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // @GetMapping("/template")
    // public ResponseEntity<byte[]> downloadSampleTemplate() {
    // try {
    // byte[] templateBytes = reportService.generateSampleTemplate();
    // String filename = "Mau_nhap_phieu_mua_sam.xlsx";
    // return ResponseEntity.ok()
    // .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
    // report.filename() + "\"")
    // .contentType(MediaType.parseMediaType(report.contentType()))
    // .body(templateBytes);
    // } catch (Exception e) {
    // log.error("Lỗi khi tạo template phiếu mua sắm: {}", e.getMessage(), e);
    // return ResponseEntity.internalServerError().build();
    // }
    // }
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportResultDto>> importTickets(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        String username = extractUsername(jwt);
        try {

            ImportResultDto result = reportService.importTickets(file, username);
            return ResponseEntity.ok(ApiResponse.success(result, "Nhập dữ liệu thành công"));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi import file Excel:", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi xử lý file Excel: " + e.getMessage()));
        }
    }
}
