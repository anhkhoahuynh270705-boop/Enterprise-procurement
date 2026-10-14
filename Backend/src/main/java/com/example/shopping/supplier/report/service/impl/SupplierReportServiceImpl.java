package com.example.shopping.supplier.report.service.impl;

import com.example.shopping.supplier.report.service.SupplierReportService;
import com.example.shopping.common.dto.response.ReportFileResponse;
import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.common.enums.ReportFormat;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopping.common.report.validation.ImportWorkbookValidator;

import com.example.shopping.supplier.dto.request.CreateSupplierRequestDto;
import com.example.shopping.supplier.dto.response.SupplierResponseDto;
import com.example.shopping.supplier.entity.SupplierEntity;
import com.example.shopping.supplier.report.dto.response.SupplierImportResultDto;
import com.example.shopping.supplier.report.dto.response.SupplierImportRowErrorDto;
import com.example.shopping.supplier.report.exporter.SupplierExcelExporter;
import com.example.shopping.supplier.report.exporter.SupplierPdfExporter;
import com.example.shopping.supplier.repository.SupplierRepository;
import com.example.shopping.supplier.service.SupplierService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierReportServiceImpl implements SupplierReportService {

    SupplierRepository supplierRepository;
    SupplierService supplierService;
    SupplierExcelExporter supplierExcelExporter;
    SupplierPdfExporter supplierPdfExporter;

    @Override
    public ReportFileResponse exportSupplier(UUID id, String format) throws Exception {
        ReportFormat reportFormat = ReportFormat.from(format);
        byte[] bytes = reportFormat == ReportFormat.EXCEL ? exportSupplierExcel(id) : exportSupplierPdf(id);
        return new ReportFileResponse("Supplier_" + id + "." + reportFormat.getExtension(), reportFormat.getContentType(), bytes);
    }

    @Override
    @Transactional
    public SupplierImportResultDto importSuppliers(MultipartFile file) throws Exception {
        ImportWorkbookValidator.validateUpload(file);
        try (var stream = file.getInputStream()) { return importSuppliersExcel(stream); }
    }

    public byte[] exportSupplierPdf(UUID id) throws Exception {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_NOT_FOUND.ex());
        return supplierPdfExporter.exportSupplier(supplier);
    }

    public byte[] exportSupplierExcel(UUID id) throws Exception {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_NOT_FOUND.ex());
        return supplierExcelExporter.exportSupplier(supplier);
    }

    public byte[] exportAllSuppliersExcel() throws Exception {
        List<SupplierEntity> suppliers = supplierRepository.findAll();
        return supplierExcelExporter.exportSuppliers(suppliers);
    }

    public byte[] generateSampleTemplate() throws Exception {
        return supplierExcelExporter.generateSampleTemplate();
    }

    @Transactional
    public SupplierImportResultDto importSuppliersExcel(InputStream inputStream) throws Exception {
        List<SupplierImportRowErrorDto> errors = new ArrayList<>();
        List<SupplierResponseDto> createdSuppliers = new ArrayList<>();

        try (Workbook workbook = ImportWorkbookValidator.open(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            var headers = ImportWorkbookValidator.validateSupplierHeaders(sheet);
            int totalRows = 0;
            int firstRow = headers.isProfile() ? headers.rowIndex() : headers.rowIndex() + 1;
            int lastRow = headers.isProfile() ? headers.rowIndex() : sheet.getLastRowNum();

            for (int i = firstRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (!headers.isProfile() && (row == null || isBlankRow(row))) {
                    continue;
                }
                totalRows++;

                String name = getCellStringValue(sheet, i, headers, "Tên nhà cung cấp");
                if (name == null || name.isBlank()) {
                    errors.add(SupplierImportRowErrorDto.builder()
                            .rowNumber(i + 1)
                            .field("Tên nhà cung cấp")
                            .errorMessage("Tên nhà cung cấp không được để trống")
                            .build());
                    continue;
                }

                String contactPerson = getCellStringValue(sheet, i, headers, "Người liên hệ");
                String email = getCellStringValue(sheet, i, headers, "Email");
                String phone = getCellStringValue(sheet, i, headers, "Số điện thoại");
                String taxCode = getCellStringValue(sheet, i, headers, "Mã số thuế");
                String address = getCellStringValue(sheet, i, headers, "Địa chỉ");
                String city = getCellStringValue(sheet, i, headers, "Thành phố");
                String country = getCellStringValue(sheet, i, headers, "Quốc gia");
                String bankName = getCellStringValue(sheet, i, headers, "Ngân hàng");
                String bankAccount = getCellStringValue(sheet, i, headers, "Số tài khoản");
                String notes = getCellStringValue(sheet, i, headers, "Ghi chú");

                if (taxCode != null && !taxCode.isBlank() && supplierRepository.existsByTaxCode(taxCode)) {
                    errors.add(SupplierImportRowErrorDto.builder()
                            .rowNumber(i + 1)
                            .field("Mã số thuế")
                            .errorMessage("Mã số thuế đã tồn tại trong hệ thống: " + taxCode)
                            .build());
                    continue;
                }

                try {
                    CreateSupplierRequestDto request = CreateSupplierRequestDto.builder()
                            .name(name)
                            .contactPerson(contactPerson)
                            .email(email)
                            .phone(phone)
                            .taxCode(taxCode)
                            .address(address)
                            .city(city)
                            .country(country != null && !country.isBlank() ? country : "Việt Nam")
                            .bankName(bankName)
                            .bankAccount(bankAccount)
                            .notes(notes)
                            .build();

                    SupplierResponseDto created = supplierService.createSupplier(request);
                    createdSuppliers.add(created);
                } catch (Exception e) {
                    errors.add(SupplierImportRowErrorDto.builder()
                            .rowNumber(i + 1)
                            .field("Lỗi hệ thống")
                            .errorMessage(e.getMessage())
                            .build());
                }
            }

            return SupplierImportResultDto.builder()
                    .totalRows(totalRows)
                    .successRows(createdSuppliers.size())
                    .errorRows(errors.size())
                    .message(errors.isEmpty()
                            ? "Nhập thành công " + createdSuppliers.size() + " nhà cung cấp!"
                            : "Nhập thành công " + createdSuppliers.size() + " nhà cung cấp, có " + errors.size() + " lỗi.")
                    .createdSuppliers(createdSuppliers)
                    .errors(errors)
                    .build();
        }
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (getCellStringValue(cell) != null && !getCellStringValue(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellStringValue(Sheet sheet, int rowIndex,
            ImportWorkbookValidator.SupplierHeaders headers, String column) {
        if (headers.isProfile()) {
            var position = headers.profileValues().get(column);
            return position == null ? null
                    : getCellStringValue(sheet.getRow(position.rowIndex()).getCell(position.columnIndex()));
        }
        Integer index = headers.columns().get(column);
        Row row = sheet.getRow(rowIndex);
        return index == null || row == null ? null : getCellStringValue(row.getCell(index));
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return null;
    }
}
