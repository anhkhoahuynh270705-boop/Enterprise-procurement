package com.example.shopping.procurement.report.service.impl;

import com.example.shopping.procurement.report.service.ReportService;
import com.example.shopping.common.dto.response.ReportFileResponse;
import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.common.enums.ReportFormat;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import com.example.shopping.common.report.validation.ImportWorkbookValidator;
import com.example.shopping.procurement.service.ProcurementService;
import com.example.shopping.procurement.service.ProcurementSkuGenerator;
import com.example.shopping.procurement.enums.ProcurementStatus;

import com.example.shopping.procurement.dto.request.CreateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.request.ProcurementItemRequestDto;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.procurement.enums.ProcurementPriority;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.entity.ProcurementItem;
import com.example.shopping.procurement.mapper.ProcurementMapper;
import com.example.shopping.procurement.report.dto.response.ImportResultDto;
import com.example.shopping.procurement.report.dto.response.ImportRowErrorDto;
import com.example.shopping.procurement.report.exporter.ExcelExporter;
import com.example.shopping.procurement.report.exporter.PdfExporter;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static java.util.UUID.randomUUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReportServiceImpl implements ReportService {

    private static final String IMPORT_DEPARTMENT = "Phòng Mua sắm";
    private static final String IMPORT_REASON = "Nhập tự động qua file Excel";

    PdfExporter pdfExporter;
    ExcelExporter excelExporter;
    ProcurementTicketRepository ticketRepository;
    ProcurementMapper procurementMapper;
    ProcurementService procurementService;
    ProcurementSkuGenerator skuGenerator;

    @Override
    @Transactional(readOnly = true)
    public ReportFileResponse exportTicket(UUID id, String format, boolean detail) throws Exception {
        ProcurementTicket ticket = procurementService.getTicketEntity(id);
        ReportFormat reportFormat = ReportFormat.from(format);
        byte[] bytes = detail
            ? (reportFormat == ReportFormat.EXCEL ? exportTicketDetailExcel(ticket) : exportTicketDetailPdf(ticket))
            : (reportFormat == ReportFormat.EXCEL ? exportTicketExcel(ticket) : exportTicketPdf(ticket));
        String filename = (detail ? "Enterprise_Detail_" : "Procurement_") + ticket.getTicketCode() + "." + reportFormat.getExtension();
        return new ReportFileResponse(filename, reportFormat.getContentType(), bytes);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportFileResponse exportAllTickets() throws Exception {
        var tickets = procurementService.getAllTickets(null, null).stream()
            .map(dto -> procurementService.getTicketEntity(dto.getId())).toList();
        return new ReportFileResponse("Danh_sach_phieu_mua_sam.xlsx", ReportFormat.EXCEL.getContentType(), exportTicketsExcel(tickets));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultDto importTickets(MultipartFile file, String makerUsername) throws Exception {
        ImportWorkbookValidator.validateUpload(file);
        try (var stream = file.getInputStream()) { return importExcelTickets(stream, file.getOriginalFilename(), makerUsername); }
    }

    public byte[] exportTicketPdf(ProcurementTicket ticket) throws Exception {
        return pdfExporter.exportTicket(ticket);
    }

    public byte[] exportTicketExcel(ProcurementTicket ticket) throws Exception {
        return excelExporter.exportTicket(ticket);
    }

    public byte[] exportTicketDetailPdf(ProcurementTicket ticket) throws Exception {
        return pdfExporter.exportTicketDetail(ticket);
    }

    public byte[] exportTicketDetailExcel(ProcurementTicket ticket) throws Exception {
        return excelExporter.exportTicketDetail(ticket);
    }

    public byte[] exportTicketsExcel(List<ProcurementTicket> tickets) throws Exception {
        return excelExporter.exportTickets(tickets);
    }

    @Override
    public byte[] generateSampleTemplate() throws Exception {
        return excelExporter.generateProcurementImportTemplate();
    }

    @Transactional(rollbackFor = Exception.class)
    public ImportResultDto importExcelTickets(InputStream inputStream, String originalFilename, String makerUsername) throws Exception {
        List<ImportRowErrorDto> errors = new ArrayList<>();
        List<ProcurementItemRequestDto> validItems = new ArrayList<>();

        try (Workbook workbook = ImportWorkbookValidator.open(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw ErrorEnum.EXCEL_NO_SHEET.ex();
            }

            Sheet sheet = workbook.getSheetAt(0);
            ImportWorkbookValidator.validateProcurementHeaders(
                    sheet, ExcelExporter.getProcurementImportHeaders());
            /* Find header row */
            int headerRowIndex = -1;
            Integer colItemName = null;
            Integer colCategory = null;
            Integer colQuantity = null;
            Integer colUnit = null;
            Integer colUnitPrice = null;
            Integer colTotalPrice = null;
            Integer colSupplier = null;
            Integer colNotes = null;

            int maxScanRows = Math.min(sheet.getLastRowNum(), 50);
            for (int i = 0; i <= maxScanRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                Integer curItemName = null;
                Integer curCategory = null;
                Integer curQuantity = null;
                Integer curUnit = null;
                Integer curUnitPrice = null;
                Integer curTotalPrice = null;
                Integer curSupplier = null;
                Integer curNotes = null;

                int lastCell = row.getLastCellNum();
                for (int c = 0; c < lastCell; c++) {
                    String val = getCellStringValue(row.getCell(c));
                    if (val == null || val.isBlank()) {
                        continue;
                    }
                    String norm = normalizeHeader(val);

                    if (curItemName == null && isItemNameHeader(norm)) {
                        curItemName = c;
                    } else if (curQuantity == null && isQuantityHeader(norm)) {
                        curQuantity = c;
                    } else if (curUnitPrice == null && isUnitPriceHeader(norm)) {
                        curUnitPrice = c;
                    } else if (curUnit == null && isUnitHeader(norm)) {
                        curUnit = c;
                    } else if (curSupplier == null && isSupplierHeader(norm)) {
                        curSupplier = c;
                    } else if (curCategory == null && isCategoryHeader(norm)) {
                        curCategory = c;
                    } else if (curTotalPrice == null && isTotalPriceHeader(norm)) {
                        curTotalPrice = c;
                    } else if (curNotes == null && isNotesHeader(norm)) {
                        curNotes = c;
                    }
                }

                if (curItemName != null && curQuantity != null && curUnitPrice != null) {
                    headerRowIndex = i;
                    colItemName = curItemName;
                    colCategory = curCategory;
                    colQuantity = curQuantity;
                    colUnit = curUnit;
                    colUnitPrice = curUnitPrice;
                    colTotalPrice = curTotalPrice;
                    colSupplier = curSupplier;
                    colNotes = curNotes;
                    break;
                }
            }

            if (headerRowIndex == -1) {
                throw ErrorEnum.EXCEL_MISSING_TEMPLATE_HEADERS.ex();
            }

            int totalRows = 0;
            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                boolean emptyRow = true;
                for (int col = 0; col < row.getLastCellNum(); col++) {
                    Cell cell = row.getCell(col);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        String s = getCellStringValue(cell);
                        if (s != null && !s.isBlank()) {
                            emptyRow = false;
                            break;
                        }
                    }
                }

                if (emptyRow) {
                    continue;
                }

                String firstVal = getCellStringValue(row.getCell(0));
                String nameVal = colItemName != null ? getCellStringValue(row.getCell(colItemName)) : null;
                if ((firstVal != null && isSummaryText(firstVal)) || (nameVal != null && isSummaryText(nameVal))) {
                    break;
                }
                if (firstVal != null && (firstVal.toLowerCase().contains("người lập") || firstVal.toLowerCase().contains("nguoi lap")
                        || firstVal.toLowerCase().contains("kiểm soát") || firstVal.toLowerCase().contains("kiem soat")
                        || firstVal.toLowerCase().contains("phê duyệt") || firstVal.toLowerCase().contains("phe duyet"))) {
                    break;
                }

                totalRows++;

                boolean rowHasErrors = false;
                String itemName = colItemName != null ? getCellStringValue(row.getCell(colItemName)) : null;
                if (itemName == null || itemName.isBlank()) {
                    addImportRowError(errors, i + 1, "Tên vật tư", "Không được để trống.");
                    rowHasErrors = true;
                }

                Integer quantity = colQuantity != null ? getCellIntegerValue(row.getCell(colQuantity)) : null;
                if (quantity == null || quantity <= 0) {
                    addImportRowError(errors, i + 1, "Số lượng", "Bắt buộc và phải là số nguyên lớn hơn 0.");
                    rowHasErrors = true;
                }

                String unitInput = colUnit != null ? getCellStringValue(row.getCell(colUnit)) : null;
                String unit = resolveProcurementUnit(unitInput);
                if (unit == null) {
                    addImportRowError(errors, i + 1, "Đơn vị tính", "Bắt buộc chọn một đơn vị trong danh sách.");
                    rowHasErrors = true;
                }

                BigDecimal unitPrice = colUnitPrice != null ? getCellBigDecimalValue(row.getCell(colUnitPrice)) : null;
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    addImportRowError(errors, i + 1, "Đơn giá", "Bắt buộc và phải lớn hơn 0.");
                    rowHasErrors = true;
                }

                String categoryInput = colCategory != null ? getCellStringValue(row.getCell(colCategory)) : null;
                String category = resolveProcurementCategory(categoryInput);
                if (category == null) {
                    addImportRowError(errors, i + 1, "Phân loại", "Bắt buộc chọn một danh mục trong danh sách.");
                    rowHasErrors = true;
                }

                String supplierName = colSupplier != null ? getCellStringValue(row.getCell(colSupplier)) : null;
                if (supplierName == null || supplierName.isBlank() || supplierName.equalsIgnoreCase("N/A")) {
                    addImportRowError(errors, i + 1, "Nhà cung cấp", "Không được để trống.");
                    rowHasErrors = true;
                } else {
                    supplierName = supplierName.trim();
                }

                if (rowHasErrors) {
                    continue;
                }

                BigDecimal totalPrice = colTotalPrice != null ? getCellBigDecimalValue(row.getCell(colTotalPrice)) : null;
                if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
                }

                String notes = colNotes != null ? getCellStringValue(row.getCell(colNotes)) : null;

                validItems.add(ProcurementItemRequestDto.builder()
                        .itemName(itemName)
                        .category(category)
                        .quantity(quantity)
                        .unit(unit)
                        .unitPrice(unitPrice)
                        .totalPrice(totalPrice)
                        .supplierName(supplierName)
                        .notes(notes)
                        .build());
            }

            ProcurementTicketResponseDto createdTicketDto = null;

            if (!validItems.isEmpty() && errors.isEmpty()) {
                String importFingerprint = fingerprintRequests(validItems);
                boolean duplicate = ticketRepository.existsByImportFingerprint(importFingerprint)
                        || ticketRepository.findImportedTicketsWithItems(IMPORT_REASON, IMPORT_DEPARTMENT).stream()
                                .anyMatch(ticket -> fingerprintTicketItems(ticket.getItems()).equals(importFingerprint));
                if (duplicate) {
                    throw ErrorEnum.TICKET_DUPLICATE_IMPORT.ex();
                }

                String ticketCode = "PR-IMP-" + randomUUID().toString();

                CreateProcurementTicketRequestDto request = CreateProcurementTicketRequestDto.builder()
                        .title((originalFilename != null ? originalFilename : "Excel"))
                        .department(IMPORT_DEPARTMENT)
                        .reason(IMPORT_REASON)
                        .currency("VND")
                        .priority(ProcurementPriority.MEDIUM)
                        .items(validItems)
                        .build();

                ProcurementTicket ticket = procurementMapper.toEntity(request, ticketCode, makerUsername);
                ticket.getItems().forEach(item -> item.setItemCode(skuGenerator.generateNextSku()));
                ticket.setImportFingerprint(importFingerprint);
                ticket.setStatus(request.isSubmitImmediately() ? ProcurementStatus.PENDING_APPROVAL : ProcurementStatus.DRAFT);
                procurementService.recalculate(ticket);
                ProcurementTicket saved;
                try {
                    saved = ticketRepository.saveAndFlush(ticket);
                } catch (DataIntegrityViolationException ex) {
                    throw ErrorEnum.TICKET_DUPLICATE_IMPORT.ex();
                }
                createdTicketDto = procurementService.submitTicket(saved.getId(), makerUsername);
            }

            String message;
            if (totalRows == 0) {
                message = "File không có dòng vật tư nào.";
            } else if (!errors.isEmpty()) {
                message = "File có " + errors.size() + " lỗi. Chưa tạo phiếu; vui lòng sửa các dòng được báo lỗi và tải lên lại.";
            } else {
                message = "Nhập thành công " + validItems.size() + " vật tư.";
            }

            return ImportResultDto.builder()
                    .totalRows(totalRows)
                    .successRows(errors.isEmpty() ? validItems.size() : 0)
                    .errorRows(errors.size())
                    .message(message)
                    .createdTicket(createdTicketDto)
                    .errors(errors)
                    .build();
        }
    }

    private String fingerprintRequests(List<ProcurementItemRequestDto> items) {
        List<String> rows = items.stream()
                .map(item -> rowFingerprint(item.getItemName(), item.getCategory(), item.getQuantity(), item.getUnit(),
                        item.getUnitPrice(), item.getSupplierName(), item.getNotes()))
                .sorted()
                .toList();
        return sha256(String.join("", rows.stream().map(this::lengthPrefix).toList()));
    }

    private String fingerprintTicketItems(List<ProcurementItem> items) {
        List<String> rows = items.stream()
                .map(item -> rowFingerprint(item.getItemName(), item.getCategory(), item.getQuantity(), item.getUnit(),
                        item.getUnitPrice(), item.getSupplierName(), item.getNotes()))
                .sorted()
                .toList();
        return sha256(String.join("", rows.stream().map(this::lengthPrefix).toList()));
    }

    private String rowFingerprint(String name, String category, Integer quantity, String unit,
            BigDecimal unitPrice, String supplier, String notes) {
        List<String> fields = List.of(normalizeFingerprintText(name), normalizeFingerprintText(category),
                quantity == null ? "" : quantity.toString(), normalizeFingerprintText(unit),
                normalizeFingerprintDecimal(unitPrice),
                normalizeFingerprintText(supplier), normalizeFingerprintText(notes));
        return String.join("", fields.stream().map(this::lengthPrefix).toList());
    }

    private String normalizeFingerprintText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeFingerprintDecimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String lengthPrefix(String value) {
        return value.length() + ":" + value;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    private String resolveProcurementCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        for (String allowedCategory : ExcelExporter.getProcurementCategoryOptions()) {
            if (allowedCategory.equalsIgnoreCase(category.trim())) {
                return allowedCategory;
            }
        }
        return null;
    }

    private String resolveProcurementUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        for (String allowedUnit : ExcelExporter.getProcurementUnitOptions()) {
            if (allowedUnit.equalsIgnoreCase(unit.trim())) {
                return allowedUnit;
            }
        }
        return null;
    }

    private void addImportRowError(List<ImportRowErrorDto> errors, int rowNumber, String field, String message) {
        errors.add(ImportRowErrorDto.builder()
                .rowNumber(rowNumber)
                .field(field)
                .errorMessage(message)
                .build());
    }

    private boolean isItemNameHeader(String h) {
        if (h.contains("nhà cung cấp") || h.contains("nha cung cap") || h.contains("ncc")
                || h.contains("đơn vị cung cấp") || h.contains("don vi cung cap")
                || h.contains("supplier") || h.contains("vendor")) {
            return false;
        }
        return h.contains("tên vật tư") || h.contains("ten vat tu")
                || h.contains("tên hàng") || h.contains("ten hang")
                || h.contains("tên sp") || h.contains("ten sp")
                || h.contains("tên sản phẩm") || h.contains("ten san pham")
                || h.contains("tên thiết bị") || h.contains("ten thiet bi")
                || h.contains("item name") || h.contains("description")
                || h.contains("mô tả") || h.contains("mo ta")
                || h.contains("mặt hàng") || h.contains("mat hang")
                || h.equals("vật tư") || h.equals("vat tu")
                || h.equals("hàng hóa") || h.equals("hang hoa")
                || h.equals("tên") || h.equals("ten");
    }

    private boolean isCategoryHeader(String h) {
        return h.contains("phân loại") || h.contains("phan loai")
                || h.contains("nhóm hàng") || h.contains("nhom hang")
                || h.contains("loại vật tư") || h.contains("loai vat tu")
                || h.contains("danh mục") || h.contains("danh muc")
                || h.contains("category")
                || h.equals("loại") || h.equals("loai");
    }

    private boolean isQuantityHeader(String h) {
        return h.contains("số lượng") || h.contains("so luong")
                || h.contains("quantity")
                || h.equals("sl") || h.equals("qty");
    }

    private boolean isUnitHeader(String h) {
        if (h.contains("giá") || h.contains("gia") || h.contains("cung cấp") || h.contains("cung cap")) {
            return false;
        }
        return h.contains("đơn vị") || h.contains("don vi")
                || h.contains("đvt") || h.contains("dvt")
                || h.contains("đơn vị tính") || h.contains("don vi tinh")
                || h.contains("unit");
    }

    private boolean isUnitPriceHeader(String h) {
        if (h.contains("thành tiền") || h.contains("thanh tien") || h.contains("tổng") || h.contains("tong") || h.contains("total")) {
            return false;
        }
        return h.contains("đơn giá") || h.contains("don gia")
                || h.contains("unit price")
                || h.equals("giá") || h.equals("gia") || h.equals("price")
                || (h.startsWith("giá") && !h.contains("trị"));
    }

    private boolean isTotalPriceHeader(String h) {
        return h.contains("thành tiền") || h.contains("thanh tien")
                || h.contains("tổng tiền") || h.contains("tong tien")
                || h.contains("total price")
                || h.equals("tổng cộng") || h.equals("tong cong")
                || h.equals("total") || h.equals("amount");
    }

    private boolean isSupplierHeader(String h) {
        return h.contains("nhà cung cấp") || h.contains("nha cung cap")
                || h.contains("nhà cung ứng") || h.contains("nha cung ung")
                || h.contains("đơn vị cung cấp") || h.contains("don vi cung cap")
                || h.contains("nhà phân phối") || h.contains("nha phan phoi")
                || h.contains("đối tác") || h.contains("doi tac")
                || h.contains("ncc")
                || h.contains("supplier") || h.contains("vendor");
    }

    private boolean isNotesHeader(String h) {
        return h.contains("ghi chú") || h.contains("ghi chu")
                || h.contains("diễn giải") || h.contains("dien giai")
                || h.contains("note") || h.contains("notes")
                || h.contains("remark") || h.contains("remarks");
    }

    private boolean isSummaryText(String text) {
        if (text == null) {
            return false;
        }
        String s = text.trim().toLowerCase();
        return s.startsWith("tổng cộng") || s.startsWith("tong cong")
                || s.startsWith("tổng kinh phí") || s.startsWith("tong kinh phi")
                || s.startsWith("tổng tiền") || s.startsWith("tong tien")
                || s.startsWith("grand total") || s.equals("tổng") || s.equals("tong");
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        if (type == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        if (type == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            if (val == (long) val) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        }
        if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return null;
    }

    private Integer getCellIntegerValue(Cell cell) {
        BigDecimal value = getCellBigDecimalValue(cell);
        if (value == null) {
            return null;
        }
        try {
            return value.intValueExact();
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal getCellBigDecimalValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            CellType type = cell.getCellType();
            if (type == CellType.FORMULA) {
                type = cell.getCachedFormulaResultType();
            }
            if (type == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (type == CellType.STRING) {
                String str = cell.getStringCellValue().trim();
                if (str.isBlank()) {
                    return null;
                }
                str = str.replaceAll("[^0-9,.-]", "").trim();
                if (str.isEmpty()) {
                    return null;
                }
                if (str.contains(",") && str.contains(".")) {
                    if (str.lastIndexOf('.') > str.lastIndexOf(',')) {
                        str = str.replace(",", "");
                    } else {
                        str = str.replace(".", "").replace(",", ".");
                    }
                } else if (str.contains(",")) {
                    long commaCount = str.chars().filter(ch -> ch == ',').count();
                    if (commaCount > 1) {
                        str = str.replace(",", "");
                    } else {
                        int commaIdx = str.indexOf(',');
                        if (str.length() - commaIdx - 1 == 3 && commaIdx > 0) {
                            str = str.replace(",", "");
                        } else {
                            str = str.replace(",", ".");
                        }
                    }
                } else if (str.contains(".")) {
                    long dotCount = str.chars().filter(ch -> ch == '.').count();
                    if (dotCount > 1) {
                        str = str.replace(".", "");
                    }
                }
                return new BigDecimal(str);
            }
        } catch (Exception ignored) {
            }
        return null;
    }
}
