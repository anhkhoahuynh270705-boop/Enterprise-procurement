package com.example.shopping.procurement.report.service.impl;

import com.example.shopping.procurement.report.service.ReportService;
import com.example.shopping.common.dto.response.ReportFileResponse;
import com.example.shopping.common.enums.ReportFormat;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopping.common.report.validation.ImportWorkbookValidator;
import com.example.shopping.procurement.service.ProcurementService;
import com.example.shopping.procurement.service.ProcurementCalculationService;
import com.example.shopping.procurement.enums.ProcurementStatus;

import com.example.shopping.procurement.dto.request.CreateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.request.ProcurementItemRequestDto;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.procurement.enums.ProcurementPriority;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.mapper.ProcurementMapper;
import com.example.shopping.procurement.report.dto.response.ImportResultDto;
import com.example.shopping.procurement.report.dto.response.ImportRowErrorDto;
import com.example.shopping.procurement.report.exporter.ExcelExporter;
import com.example.shopping.procurement.report.exporter.PdfExporter;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final PdfExporter pdfExporter;
    private final ExcelExporter excelExporter;
    private final ProcurementTicketRepository ticketRepository;
    private final ProcurementMapper procurementMapper;
    private final ProcurementService procurementService;
    private final ProcurementCalculationService calculations;

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

    // public byte[] generateSampleTemplate() throws Exception {
    //     return excelExporter.generateSampleTemplate();
    // }

    @Transactional(rollbackFor = Exception.class)
    public ImportResultDto importExcelTickets(InputStream inputStream, String originalFilename, String makerUsername) throws Exception {
        List<ImportRowErrorDto> errors = new ArrayList<>();
        List<ProcurementItemRequestDto> validItems = new ArrayList<>();

        try (Workbook workbook = ImportWorkbookValidator.open(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("File Excel không có sheet nào.");
            }

            Sheet sheet = workbook.getSheetAt(0);
            /* Find header row */
            int headerRowIndex = -1;
            Integer colItemCode = null;
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

                Integer curItemCode = null;
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
                    } else if (curItemCode == null && isItemCodeHeader(norm)) {
                        curItemCode = c;
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
                    colItemCode = curItemCode;
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
                throw new IllegalArgumentException(
                        "File import không hợp lệ. Vui lòng dùng mẫu vật tư."
                );
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

                String itemName = colItemName != null ? getCellStringValue(row.getCell(colItemName)) : null;
                if (itemName == null || itemName.isBlank()) {
                    errors.add(ImportRowErrorDto.builder()
                            .rowNumber(i + 1)
                            .field("Tên vật tư")
                            .errorMessage("Tên vật tư không được để trống")
                            .build());
                    continue;
                }

                Integer quantity = colQuantity != null ? getCellIntegerValue(row.getCell(colQuantity)) : null;
                if (quantity == null || quantity <= 0) {
                    errors.add(ImportRowErrorDto.builder()
                            .rowNumber(i + 1)
                            .field("Số lượng")
                            .errorMessage("Số lượng phải là số nguyên lớn hơn 0")
                            .build());
                    continue;
                }

                String unit = colUnit != null ? getCellStringValue(row.getCell(colUnit)) : null;
                if (unit == null || unit.isBlank()) {
                    unit = "Cái";
                }

                BigDecimal unitPrice = colUnitPrice != null ? getCellBigDecimalValue(row.getCell(colUnitPrice)) : null;
                if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(ImportRowErrorDto.builder()
                            .rowNumber(i + 1)
                            .field("Đơn giá")
                            .errorMessage("Đơn giá phải lớn hơn 0")
                            .build());
                    continue;
                }

                BigDecimal totalPrice = colTotalPrice != null ? getCellBigDecimalValue(row.getCell(colTotalPrice)) : null;
                if (totalPrice == null || totalPrice.compareTo(BigDecimal.ZERO) <= 0) {
                    totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
                }

                String itemCode = colItemCode != null ? getCellStringValue(row.getCell(colItemCode)) : null;
                String category = colCategory != null ? getCellStringValue(row.getCell(colCategory)) : null;
                String supplierName = colSupplier != null ? getCellStringValue(row.getCell(colSupplier)) : null;
                if (supplierName != null && (supplierName.isBlank() || supplierName.equalsIgnoreCase("N/A"))) {
                    supplierName = null;
                }
                String notes = colNotes != null ? getCellStringValue(row.getCell(colNotes)) : null;

                validItems.add(ProcurementItemRequestDto.builder()
                        .itemCode(itemCode)
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

            if (!validItems.isEmpty()) {
                String ticketCode = "PR-IMP-" + java.util.UUID.randomUUID().toString();

                CreateProcurementTicketRequestDto request = CreateProcurementTicketRequestDto.builder()
                        .title((originalFilename != null ? originalFilename : "Excel"))
                        .department("Phòng Mua sắm")
                        .reason("Nhập tự động qua file Excel")
                        .currency("VND")
                        .priority(ProcurementPriority.MEDIUM)
                        .items(validItems)
                        .build();

                ProcurementTicket ticket = procurementMapper.toEntity(request, ticketCode, makerUsername);
                ticket.setStatus(request.isSubmitImmediately() ? ProcurementStatus.PENDING_APPROVAL : ProcurementStatus.DRAFT);
                calculations.recalculate(ticket);
                ProcurementTicket saved = ticketRepository.save(ticket);
                createdTicketDto = procurementService.submitTicket(saved.getId(), makerUsername);
            }

            String message;
            if (totalRows == 0) {
                message = "File không có dòng vật tư nào.";
            } else {
                message = "Nhập thành công " + validItems.size() + " vật tư, có " + errors.size() + " dòng lỗi.";
            }

            return ImportResultDto.builder()
                    .totalRows(totalRows)
                    .successRows(validItems.size())
                    .errorRows(errors.size())
                    .message(message)
                    .createdTicket(createdTicketDto)
                    .errors(errors)
                    .build();
        }
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toLowerCase().replaceAll("\\s+", " ");
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

    private boolean isItemCodeHeader(String h) {
        return h.contains("mã vt") || h.contains("ma vt")
                || h.contains("mã vật tư") || h.contains("ma vat tu")
                || h.contains("mã hàng") || h.contains("ma hang")
                || h.contains("mã sp") || h.contains("ma sp")
                || h.contains("mã sản phẩm") || h.contains("ma san pham")
                || h.contains("mã sku") || h.contains("ma sku")
                || h.contains("sku")
                || h.contains("item code")
                || h.equals("mã") || h.equals("ma")
                || h.equals("code");
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
            return value.intValue();
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
