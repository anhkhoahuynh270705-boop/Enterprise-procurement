package com.example.shopping.supplier.report.service;

import java.io.InputStream;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import com.example.shopping.common.dto.response.ReportFileResponse;
import com.example.shopping.supplier.report.dto.response.SupplierImportResultDto;

public interface SupplierReportService {
    ReportFileResponse exportSupplier(UUID id, String format) throws Exception;
    SupplierImportResultDto importSuppliers(MultipartFile file) throws Exception;
    byte[] exportSupplierPdf(UUID id) throws Exception;
    byte[] exportSupplierExcel(UUID id) throws Exception;
    byte[] exportAllSuppliersExcel() throws Exception;
    byte[] generateSampleTemplate() throws Exception;
    SupplierImportResultDto importSuppliersExcel(InputStream inputStream) throws Exception;
}
