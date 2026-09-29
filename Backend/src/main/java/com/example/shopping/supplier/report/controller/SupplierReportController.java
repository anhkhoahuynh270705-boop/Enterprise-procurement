package com.example.shopping.supplier.report.controller;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.shopping.common.enums.ReportFormat;

import com.example.shopping.common.dto.response.ApiResponse;
import com.example.shopping.supplier.report.dto.response.SupplierImportResultDto;
import com.example.shopping.supplier.report.service.SupplierReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierReportController {

    private final SupplierReportService supplierReportService;

    /**
     * Xuất chi tiết hồ sơ 1 nhà cung cấp
     */
    @GetMapping("/export/{id}")
    public ResponseEntity<byte[]> exportSupplier(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "PDF") String format) {
        try {
            var report = supplierReportService.exportSupplier(id, format);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.filename() + "\"")
                    .contentType(MediaType.parseMediaType(report.contentType()))
                    .body(report.bytes());
        } catch (Exception e) {
            log.error("Lỗi khi xuất hồ sơ nhà cung cấp:", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Xuất danh sách toàn bộ nhà cung cấp ra EXCEL
     */
    @GetMapping("/export-all")
    public ResponseEntity<byte[]> exportAllSuppliers(
            @RequestParam(defaultValue = "EXCEL") String format) {
        try {
            byte[] reportBytes = supplierReportService.exportAllSuppliersExcel();
            String filename = "Danh_sach_nha_cung_cap.xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(ReportFormat.EXCEL.getContentType()))
                    .body(reportBytes);
        } catch (Exception e) {
            log.error("Lỗi khi xuất danh sách nhà cung cấp: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Tải file Excel mẫu để import nhà cung cấp
     */
    // @GetMapping("/template")
    // public ResponseEntity<byte[]> downloadSampleTemplate() {
    //     try {
    //         byte[] templateBytes = supplierReportService.generateSampleTemplate();
    //         return ResponseEntity.ok()
    //                 .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Mau_nhap_nha_cung_cap.xlsx\"")
    //                 .contentType(MediaType.parseMediaType(ReportFormat.EXCEL.getContentType()))
    //                 .body(templateBytes);
    //     } catch (Exception e) {
    //         log.error("Lỗi khi tạo template nhà cung cấp: {}", e.getMessage(), e);
    //         return ResponseEntity.internalServerError().build();
    //     }
    // }

    /**
     * Import nhà cung cấp từ file Excel
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SupplierImportResultDto>> importSuppliers(
            @RequestParam("file") MultipartFile file) {
        try {

            SupplierImportResultDto result = supplierReportService.importSuppliers(file);
            return ResponseEntity.ok(ApiResponse.success(result, "Nhập danh sách nhà cung cấp thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi import file nhà cung cấp: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Lỗi xử lý file Excel: " + e.getMessage()));
        }
    }
}
