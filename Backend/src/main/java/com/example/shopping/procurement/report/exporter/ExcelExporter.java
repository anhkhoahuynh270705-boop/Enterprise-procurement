package com.example.shopping.procurement.report.exporter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.example.shopping.procurement.entity.ProcurementItem;
import com.example.shopping.procurement.entity.ProcurementTicket;

import lombok.extern.slf4j.Slf4j;
import com.example.shopping.user.repository.UserRepository;
import com.example.shopping.user.entity.UserEntity;

@Slf4j
@Component
@lombok.RequiredArgsConstructor
public class ExcelExporter {

    private final UserRepository users;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String getFormat() {
        return "EXCEL";
    }

    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    public String getFileExtension() {
        return "xlsx";
    }
    /* export one ticket for each record*/
    public byte[] exportTicket(ProcurementTicket ticket) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Chi tiết phiếu " + ticket.getTicketCode());
            sheet.setDisplayGridlines(true);

            DataFormat dataFormat = workbook.createDataFormat();

            Font titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font labelFont = workbook.createFont();
            labelFont.setFontName("Calibri");
            labelFont.setBold(true);

            CellStyle labelStyle = workbook.createCellStyle();
            labelStyle.setFont(labelFont);

            CellStyle valueStyle = workbook.createCellStyle();
            Font regularFont = workbook.createFont();
            regularFont.setFontName("Calibri");
            valueStyle.setFont(regularFont);

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setFont(regularFont);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setFont(regularFont);
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            numberStyle.setBorderTop(BorderStyle.THIN);
            numberStyle.setBorderBottom(BorderStyle.THIN);
            numberStyle.setBorderLeft(BorderStyle.THIN);
            numberStyle.setBorderRight(BorderStyle.THIN);

            Row titleRow = sheet.createRow(1);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("PHIẾU MUA SẮM VẬT TƯ: " + ticket.getTicketCode());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

            int rowIdx = 3;
            rowIdx = addInfoRow(sheet, rowIdx, "Tiêu đề:", ticket.getTitle(), "Trạng thái:", ticket.getStatus() != null ? ticket.getStatus().name() : "", labelStyle, valueStyle);
            rowIdx = addInfoRow(sheet, rowIdx, "Bộ phận:", ticket.getDepartment(), "Mức ưu tiên:", ticket.getPriority() != null ? ticket.getPriority().name() : "", labelStyle, valueStyle);
            rowIdx = addInfoRow(sheet, rowIdx, "Người đề xuất:", fullName(ticket.getMakerUsername(), "Chưa cập nhật họ tên"), "Người duyệt:", fullName(ticket.getCheckerUsername(), "Chưa phê duyệt"), labelStyle, valueStyle);
            rowIdx = addInfoRow(sheet, rowIdx, "Lý do:", ticket.getReason(), "Thời gian duyệt:", ticket.getApprovedAt() != null ? ticket.getApprovedAt().format(dateFormatter) : "N/A", labelStyle, valueStyle);

            rowIdx++;
            Row th = sheet.createRow(rowIdx++);
            th.setHeightInPoints(24);
            String[] headers = {
                "STT", "Mã VT", "Tên vật tư", "Phân loại", "Đơn vị", "Số lượng",
                "Đơn giá (" + ticket.getCurrency() + ")", "Thành tiền (" + ticket.getCurrency() + ")",
                "Nhà cung cấp dự kiến", "Ghi chú"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell c = th.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int stt = 1;
            BigDecimal grandTotal = BigDecimal.ZERO;
            if (ticket.getItems() != null) {
                for (ProcurementItem item : ticket.getItems()) {
                    Row r = sheet.createRow(rowIdx++);
                    r.createCell(0).setCellValue(stt++);
                    r.getCell(0).setCellStyle(dataStyle);

                    r.createCell(1).setCellValue(item.getItemCode() != null ? item.getItemCode() : "");
                    r.getCell(1).setCellStyle(dataStyle);

                    r.createCell(2).setCellValue(item.getItemName() != null ? item.getItemName() : "");
                    r.getCell(2).setCellStyle(dataStyle);

                    r.createCell(3).setCellValue(item.getCategory() != null ? item.getCategory() : "");
                    r.getCell(3).setCellStyle(dataStyle);

                    r.createCell(4).setCellValue(item.getUnit() != null ? item.getUnit() : "");
                    r.getCell(4).setCellStyle(dataStyle);

                    Cell qCell = r.createCell(5);
                    qCell.setCellValue(item.getQuantity() != null ? item.getQuantity() : 0);
                    qCell.setCellStyle(dataStyle);

                    Cell priceCell = r.createCell(6);
                    priceCell.setCellValue(item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0);
                    priceCell.setCellStyle(numberStyle);

                    Cell totalCell = r.createCell(7);
                    totalCell.setCellValue(item.getTotalPrice() != null ? item.getTotalPrice().doubleValue() : 0.0);
                    totalCell.setCellStyle(numberStyle);

                    r.createCell(8).setCellValue(item.getSupplierName() != null ? item.getSupplierName() : "");
                    r.getCell(8).setCellStyle(dataStyle);

                    r.createCell(9).setCellValue(item.getNotes() != null ? item.getNotes() : "");
                    r.getCell(9).setCellStyle(dataStyle);

                    if (item.getTotalPrice() != null) {
                        grandTotal = grandTotal.add(item.getTotalPrice());
                    }
                }
            }

            Row totalRow = sheet.createRow(rowIdx++);
            totalRow.createCell(0).setCellValue("TỔNG CỘNG:");
            totalRow.getCell(0).setCellStyle(labelStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 6));

            Cell totalValCell = totalRow.createCell(7);
            totalValCell.setCellValue(grandTotal.doubleValue());
            totalValCell.setCellStyle(numberStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
    /* export more tickets */

    public byte[] exportTickets(List<ProcurementTicket> tickets) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh sách phiếu mua sắm");
            sheet.setDisplayGridlines(true);

            DataFormat dataFormat = workbook.createDataFormat();

            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Font regularFont = workbook.createFont();
            regularFont.setFontName("Calibri");

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setFont(regularFont);

            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setFont(regularFont);
            numberStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);

            String[] headers = {"STT", "Mã phiếu", "Tiêu đề", "Bộ phận", "Người tạo", "Tổng tiền", "Tiền tệ", "Trạng thái", "Độ ưu tiên", "Ngày tạo"};
            Row th = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = th.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            int stt = 1;
            for (ProcurementTicket ticket : tickets) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(stt++);
                r.createCell(1).setCellValue(ticket.getTicketCode());
                r.createCell(2).setCellValue(ticket.getTitle());
                r.createCell(3).setCellValue(ticket.getDepartment() != null ? ticket.getDepartment() : "");
                r.createCell(4).setCellValue(fullName(ticket.getMakerUsername(), "Chưa cập nhật họ tên"));

                Cell amountCell = r.createCell(5);
                amountCell.setCellValue(ticket.getTotalAmount() != null ? ticket.getTotalAmount().doubleValue() : 0.0);
                amountCell.setCellStyle(numberStyle);

                r.createCell(6).setCellValue(ticket.getCurrency() != null ? ticket.getCurrency() : "VND");
                r.createCell(7).setCellValue(ticket.getStatus() != null ? ticket.getStatus().name() : "");
                r.createCell(8).setCellValue(ticket.getPriority() != null ? ticket.getPriority().name() : "");
                r.createCell(9).setCellValue(ticket.getCreatedAt() != null ? ticket.getCreatedAt().format(dateFormatter) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Xuất Excel Enterprise Procurement Detail
     */
    public byte[] exportTicketDetail(ProcurementTicket ticket) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String currency = ticket.getCurrency() != null ? ticket.getCurrency() : "VND";
            Sheet sheet = workbook.createSheet("Enterprise Detail");
            sheet.setDisplayGridlines(true);

            DataFormat dataFormat = workbook.createDataFormat();

            Font brandFont = workbook.createFont();
            brandFont.setFontName("Calibri");
            brandFont.setFontHeightInPoints((short) 10);
            brandFont.setBold(true);
            brandFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            CellStyle brandStyle = workbook.createCellStyle();
            brandStyle.setFont(brandFont);
            brandStyle.setAlignment(HorizontalAlignment.CENTER);

            Font titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font subFont = workbook.createFont();
            subFont.setFontName("Calibri");
            subFont.setFontHeightInPoints((short) 10);
            subFont.setItalic(true);
            subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

            CellStyle subStyle = workbook.createCellStyle();
            subStyle.setFont(subFont);
            subStyle.setAlignment(HorizontalAlignment.CENTER);

            Font sectionFont = workbook.createFont();
            sectionFont.setFontName("Calibri");
            sectionFont.setFontHeightInPoints((short) 11);
            sectionFont.setBold(true);
            sectionFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle sectionStyle = workbook.createCellStyle();
            sectionStyle.setFont(sectionFont);
            sectionStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sectionStyle.setAlignment(HorizontalAlignment.LEFT);
            sectionStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font labelFont = workbook.createFont();
            labelFont.setFontName("Calibri");
            labelFont.setFontHeightInPoints((short) 10);
            labelFont.setBold(true);

            CellStyle labelStyle = workbook.createCellStyle();
            labelStyle.setFont(labelFont);
            labelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            labelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            labelStyle.setBorderTop(BorderStyle.THIN);
            labelStyle.setBorderBottom(BorderStyle.THIN);
            labelStyle.setBorderLeft(BorderStyle.THIN);
            labelStyle.setBorderRight(BorderStyle.THIN);
            labelStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Font regularFont = workbook.createFont();
            regularFont.setFontName("Calibri");
            regularFont.setFontHeightInPoints((short) 10);

            CellStyle valueStyle = workbook.createCellStyle();
            valueStyle.setFont(regularFont);
            valueStyle.setBorderTop(BorderStyle.THIN);
            valueStyle.setBorderBottom(BorderStyle.THIN);
            valueStyle.setBorderLeft(BorderStyle.THIN);
            valueStyle.setBorderRight(BorderStyle.THIN);
            valueStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle valueBoldStyle = workbook.createCellStyle();
            Font boldValFont = workbook.createFont();
            boldValFont.setFontName("Calibri");
            boldValFont.setFontHeightInPoints((short) 10);
            boldValFont.setBold(true);
            valueBoldStyle.setFont(boldValFont);
            valueBoldStyle.setBorderTop(BorderStyle.THIN);
            valueBoldStyle.setBorderBottom(BorderStyle.THIN);
            valueBoldStyle.setBorderLeft(BorderStyle.THIN);
            valueBoldStyle.setBorderRight(BorderStyle.THIN);
            valueBoldStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle thStyle = workbook.createCellStyle();
            thStyle.setFont(sectionFont);
            thStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            thStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            thStyle.setAlignment(HorizontalAlignment.CENTER);
            thStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            thStyle.setBorderTop(BorderStyle.THIN);
            thStyle.setBorderBottom(BorderStyle.THIN);
            thStyle.setBorderLeft(BorderStyle.THIN);
            thStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataCenterStyle = workbook.createCellStyle();
            dataCenterStyle.setFont(regularFont);
            dataCenterStyle.setAlignment(HorizontalAlignment.CENTER);
            dataCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataCenterStyle.setBorderTop(BorderStyle.THIN);
            dataCenterStyle.setBorderBottom(BorderStyle.THIN);
            dataCenterStyle.setBorderLeft(BorderStyle.THIN);
            dataCenterStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataLeftStyle = workbook.createCellStyle();
            dataLeftStyle.setFont(regularFont);
            dataLeftStyle.setAlignment(HorizontalAlignment.LEFT);
            dataLeftStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataLeftStyle.setBorderTop(BorderStyle.THIN);
            dataLeftStyle.setBorderBottom(BorderStyle.THIN);
            dataLeftStyle.setBorderLeft(BorderStyle.THIN);
            dataLeftStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataLeftBoldStyle = workbook.createCellStyle();
            dataLeftBoldStyle.setFont(boldValFont);
            dataLeftBoldStyle.setAlignment(HorizontalAlignment.LEFT);
            dataLeftBoldStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataLeftBoldStyle.setBorderTop(BorderStyle.THIN);
            dataLeftBoldStyle.setBorderBottom(BorderStyle.THIN);
            dataLeftBoldStyle.setBorderLeft(BorderStyle.THIN);
            dataLeftBoldStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataNumberStyle = workbook.createCellStyle();
            dataNumberStyle.setFont(regularFont);
            dataNumberStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            dataNumberStyle.setAlignment(HorizontalAlignment.RIGHT);
            dataNumberStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataNumberStyle.setBorderTop(BorderStyle.THIN);
            dataNumberStyle.setBorderBottom(BorderStyle.THIN);
            dataNumberStyle.setBorderLeft(BorderStyle.THIN);
            dataNumberStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataNumberBoldStyle = workbook.createCellStyle();
            dataNumberBoldStyle.setFont(boldValFont);
            dataNumberBoldStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            dataNumberBoldStyle.setAlignment(HorizontalAlignment.RIGHT);
            dataNumberBoldStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataNumberBoldStyle.setBorderTop(BorderStyle.THIN);
            dataNumberBoldStyle.setBorderBottom(BorderStyle.THIN);
            dataNumberBoldStyle.setBorderLeft(BorderStyle.THIN);
            dataNumberBoldStyle.setBorderRight(BorderStyle.THIN);

            CellStyle totalLabelStyle = workbook.createCellStyle();
            totalLabelStyle.setFont(boldValFont);
            totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalLabelStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            totalLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalLabelStyle.setBorderTop(BorderStyle.THIN);
            totalLabelStyle.setBorderBottom(BorderStyle.DOUBLE);
            totalLabelStyle.setBorderLeft(BorderStyle.THIN);
            totalLabelStyle.setBorderRight(BorderStyle.THIN);

            CellStyle totalValueStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setFontName("Calibri");
            totalFont.setFontHeightInPoints((short) 11);
            totalFont.setBold(true);
            totalFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            totalValueStyle.setFont(totalFont);
            totalValueStyle.setDataFormat(dataFormat.getFormat("#,##0"));
            totalValueStyle.setAlignment(HorizontalAlignment.RIGHT);
            totalValueStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            totalValueStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            totalValueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            totalValueStyle.setBorderTop(BorderStyle.THIN);
            totalValueStyle.setBorderBottom(BorderStyle.DOUBLE);
            totalValueStyle.setBorderLeft(BorderStyle.THIN);
            totalValueStyle.setBorderRight(BorderStyle.THIN);

            Row r0 = sheet.createRow(0);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("ENTERPRISE PROCUREMENT MANAGEMENT");
            c0.setCellStyle(brandStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            Row r1 = sheet.createRow(1);
            r1.setHeightInPoints(28);
            Cell c1 = r1.createCell(0);
            c1.setCellValue("BẢNG CHI TIẾT PHIẾU MUA SẮM (ENTERPRISE PROCUREMENT DETAIL)");
            c1.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

            Row r2 = sheet.createRow(2);
            Cell c2 = r2.createCell(0);
            c2.setCellValue("Mã phiếu: " + ticket.getTicketCode() + "   |   Trạng thái: " + (ticket.getStatus() != null ? ticket.getStatus().name() : "N/A"));
            c2.setCellStyle(subStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 9));

            Row rSec1 = sheet.createRow(4);
            rSec1.setHeightInPoints(22);
            Cell cSec1 = rSec1.createCell(0);
            cSec1.setCellValue("  01. THÔNG TIN PHIẾU ĐỀ NGHỊ & PHÊ DUYỆT");
            cSec1.setCellStyle(sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 9));

            // Metadata rows
            int rIdx = 5;
            Row rInfo1 = sheet.createRow(rIdx++);
            setCell(rInfo1, 0, "Mã phiếu:", labelStyle);
            setCell(rInfo1, 1, ticket.getTicketCode(), valueBoldStyle);
            setCell(rInfo1, 3, "Trạng thái:", labelStyle);
            setCell(rInfo1, 4, ticket.getStatus() != null ? ticket.getStatus().name() : "", valueBoldStyle);
            setCell(rInfo1, 6, "Độ ưu tiên:", labelStyle);
            setCell(rInfo1, 7, ticket.getPriority() != null ? ticket.getPriority().name() : "", valueBoldStyle);

            Row rInfo2 = sheet.createRow(rIdx++);
            setCell(rInfo2, 0, "Tiêu đề:", labelStyle);
            setCell(rInfo2, 1, ticket.getTitle(), valueStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 1, 4));
            setCell(rInfo2, 6, "Phòng ban:", labelStyle);
            setCell(rInfo2, 7, ticket.getDepartment() != null ? ticket.getDepartment() : "N/A", valueStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 7, 9));

            Row rInfo3 = sheet.createRow(rIdx++);
            setCell(rInfo3, 0, "Người lập (Maker):", labelStyle);
            setCell(rInfo3, 1, fullName(ticket.getMakerUsername(), "Chưa cập nhật họ tên"), valueStyle);
            setCell(rInfo3, 3, "Ngày tạo:", labelStyle);
            setCell(rInfo3, 4, ticket.getCreatedAt() != null ? ticket.getCreatedAt().format(dateFormatter) : "N/A", valueStyle);
            setCell(rInfo3, 6, "Đơn vị tiền tệ:", labelStyle);
            setCell(rInfo3, 7, currency, valueBoldStyle);

            Row rInfo4 = sheet.createRow(rIdx++);
            setCell(rInfo4, 0, "Người duyệt (Checker):", labelStyle);
            setCell(rInfo4, 1, fullName(ticket.getCheckerUsername(), "Chưa phê duyệt"), valueStyle);
            setCell(rInfo4, 3, "Ngày duyệt:", labelStyle);
            setCell(rInfo4, 4, ticket.getApprovedAt() != null ? ticket.getApprovedAt().format(dateFormatter) : "N/A", valueStyle);
            setCell(rInfo4, 6, "Mã quy trình:", labelStyle);
            setCell(rInfo4, 7, ticket.getCamundaProcessInstanceId() != null ? ticket.getCamundaProcessInstanceId() : "N/A", valueStyle);

            Row rInfo5 = sheet.createRow(rIdx++);
            setCell(rInfo5, 0, "Lý do mua sắm:", labelStyle);
            setCell(rInfo5, 1, ticket.getReason() != null ? ticket.getReason() : "", valueStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 1, 9));

            Row rInfo6 = sheet.createRow(rIdx++);
            setCell(rInfo6, 0, "Ý kiến kiểm duyệt:", labelStyle);
            setCell(rInfo6, 1, ticket.getCheckerComment() != null ? ticket.getCheckerComment() : "Không có", valueStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 1, 9));

            rIdx++;

            Row rSec2 = sheet.createRow(rIdx++);
            rSec2.setHeightInPoints(22);
            Cell cSec2 = rSec2.createCell(0);
            cSec2.setCellValue("  02. DANH MỤC CHI TIẾT VẬT TƯ / THIẾT BỊ");
            cSec2.setCellStyle(sectionStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 9));

            String[] headers = {
                "STT", "Mã VT (SKU)", "Tên hàng hóa / Vật tư", "Phân loại", "ĐVT",
                "Số lượng", "Đơn giá (" + currency + ")", "Thành tiền (" + currency + ")",
                "Nhà cung cấp dự kiến", "Ghi chú"
            };
            Row th = sheet.createRow(rIdx++);
            th.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell c = th.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(thStyle);
            }

            // Data Rows
            int stt = 1;
            BigDecimal grandTotal = BigDecimal.ZERO;
            if (ticket.getItems() != null) {
                for (ProcurementItem item : ticket.getItems()) {
                    Row r = sheet.createRow(rIdx++);
                    r.setHeightInPoints(20);
                    setCell(r, 0, String.valueOf(stt++), dataCenterStyle);
                    setCell(r, 1, item.getItemCode() != null ? item.getItemCode() : "", dataCenterStyle);
                    setCell(r, 2, item.getItemName() != null ? item.getItemName() : "", dataLeftBoldStyle);
                    setCell(r, 3, item.getCategory() != null ? item.getCategory() : "", dataLeftStyle);
                    setCell(r, 4, item.getUnit() != null ? item.getUnit() : "", dataCenterStyle);
                    Cell qCell = r.createCell(5);
                    qCell.setCellValue(item.getQuantity() != null ? item.getQuantity() : 0);
                    qCell.setCellStyle(dataCenterStyle);
                    Cell pCell = r.createCell(6);
                    pCell.setCellValue(item.getUnitPrice() != null ? item.getUnitPrice().doubleValue() : 0.0);
                    pCell.setCellStyle(dataNumberStyle);
                    Cell tCell = r.createCell(7);
                    tCell.setCellValue(item.getTotalPrice() != null ? item.getTotalPrice().doubleValue() : 0.0);
                    tCell.setCellStyle(dataNumberBoldStyle);
                    setCell(r, 8, item.getSupplierName() != null ? item.getSupplierName() : "", dataLeftStyle);
                    setCell(r, 9, item.getNotes() != null ? item.getNotes() : "", dataLeftStyle);

                    if (item.getTotalPrice() != null) {
                        grandTotal = grandTotal.add(item.getTotalPrice());
                    }
                }
            }
            Row rTotal = sheet.createRow(rIdx++);
            rTotal.setHeightInPoints(26);
            for (int i = 0; i <= 6; i++) {
                Cell c = rTotal.createCell(i);
                c.setCellStyle(totalLabelStyle);
            }
            rTotal.getCell(0).setCellValue("TỔNG CỘNG KINH PHÍ DỰ TOÁN (" + currency + "):");
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 6));

            Cell cTotalVal = rTotal.createCell(7);
            cTotalVal.setCellValue(grandTotal.doubleValue());
            cTotalVal.setCellStyle(totalValueStyle);

            for (int i = 8; i <= 9; i++) {
                Cell c = rTotal.createCell(i);
                c.setCellStyle(totalLabelStyle);
            }
            rIdx += 2;
            Row rSignTitle = sheet.createRow(rIdx++);
            rSignTitle.setHeightInPoints(22);

            Cell cSign1 = rSignTitle.createCell(0);
            cSign1.setCellValue("NGƯỜI LẬP PHIẾU");
            cSign1.setCellStyle(thStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 2));

            Cell cSign2 = rSignTitle.createCell(3);
            cSign2.setCellValue("KIỂM SOÁT NGÂN SÁCH");
            cSign2.setCellStyle(thStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 3, 6));

            Cell cSign3 = rSignTitle.createCell(7);
            cSign3.setCellValue("NGƯỜI PHÊ DUYỆT");
            cSign3.setCellStyle(thStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 7, 9));

            Row rSignNote = sheet.createRow(rIdx++);
            setCell(rSignNote, 0, "(Ký và ghi rõ họ tên)", subStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 2));
            setCell(rSignNote, 3, "(Xác nhận hạn mức kinh phí)", subStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 3, 6));
            setCell(rSignNote, 7, "(Ký và ghi rõ họ tên)", subStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 7, 9));

            rIdx += 3; 

            Row rSignName = sheet.createRow(rIdx++);
            setCell(rSignName, 0, fullName(ticket.getMakerUsername(), "Chưa cập nhật họ tên"), brandStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 0, 2));
            setCell(rSignName, 3, "Đã thẩm tra ngân sách", brandStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 3, 6));
            setCell(rSignName, 7, fullName(ticket.getCheckerUsername(), "Chưa phê duyệt"), brandStyle);
            sheet.addMergedRegion(new CellRangeAddress(rIdx - 1, rIdx - 1, 7, 9));

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) < 3000) {
                    sheet.setColumnWidth(i, 3500);
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // /**
    //  * Tạo file Excel mẫu chuẩn cho việc import danh mục vật tư
    //  */
    // public byte[] generateSampleTemplate() throws Exception {
    //     try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
    //         Sheet sheet = workbook.createSheet("Mau_Nhap_Vat_Tu");
    //         sheet.setDisplayGridlines(true);

    //         Font headerFont = workbook.createFont();
    //         headerFont.setFontName("Calibri");
    //         headerFont.setFontHeightInPoints((short) 11);
    //         headerFont.setBold(true);
    //         headerFont.setColor(IndexedColors.WHITE.getIndex());

    //         CellStyle headerStyle = workbook.createCellStyle();
    //         headerStyle.setFont(headerFont);
    //         headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
    //         headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    //         headerStyle.setAlignment(HorizontalAlignment.CENTER);
    //         headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    //         headerStyle.setBorderTop(BorderStyle.THIN);
    //         headerStyle.setBorderBottom(BorderStyle.THIN);
    //         headerStyle.setBorderLeft(BorderStyle.THIN);
    //         headerStyle.setBorderRight(BorderStyle.THIN);

    //         Font regularFont = workbook.createFont();
    //         regularFont.setFontName("Calibri");

    //         CellStyle dataLeftStyle = workbook.createCellStyle();
    //         dataLeftStyle.setFont(regularFont);
    //         dataLeftStyle.setBorderTop(BorderStyle.THIN);
    //         dataLeftStyle.setBorderBottom(BorderStyle.THIN);
    //         dataLeftStyle.setBorderLeft(BorderStyle.THIN);
    //         dataLeftStyle.setBorderRight(BorderStyle.THIN);

    //         CellStyle dataCenterStyle = workbook.createCellStyle();
    //         dataCenterStyle.setFont(regularFont);
    //         dataCenterStyle.setAlignment(HorizontalAlignment.CENTER);
    //         dataCenterStyle.setBorderTop(BorderStyle.THIN);
    //         dataCenterStyle.setBorderBottom(BorderStyle.THIN);
    //         dataCenterStyle.setBorderLeft(BorderStyle.THIN);
    //         dataCenterStyle.setBorderRight(BorderStyle.THIN);

    //         CellStyle dataNumberStyle = workbook.createCellStyle();
    //         dataNumberStyle.setFont(regularFont);
    //         dataNumberStyle.setAlignment(HorizontalAlignment.RIGHT);
    //         dataNumberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
    //         dataNumberStyle.setBorderTop(BorderStyle.THIN);
    //         dataNumberStyle.setBorderBottom(BorderStyle.THIN);
    //         dataNumberStyle.setBorderLeft(BorderStyle.THIN);
    //         dataNumberStyle.setBorderRight(BorderStyle.THIN);

    //         Row header = sheet.createRow(0);
    //         header.setHeightInPoints(26);
    //         String[] columns = {
    //             "STT", "Mã VT (SKU)", "Tên vật tư (*)", "Phân loại", "Đơn vị tính (*)",
    //             "Số lượng (*)", "Đơn giá (*)", "Thành tiền", "Nhà cung cấp", "Ghi chú"
    //         };
    //         for (int i = 0; i < columns.length; i++) {
    //             Cell cell = header.createCell(i);
    //             cell.setCellValue(columns[i]);
    //             cell.setCellStyle(headerStyle);
    //         }

    //         // Dòng dữ liệu mẫu
    //         Object[][] samples = {
    //             {1, "LAP-DELL-5520", "Laptop Dell Latitude 5520 16GB", "Thiết bị CNTT", "Chiếc", 5, 22000000.0, 110000000.0, "Công ty CP Tin Học Sao Mai", "Dành cho nhân viên mới"},
    //             {2, "MON-DELL-P24", "Màn hình Dell P2422H 23.8 inch", "Thiết bị CNTT", "Chiếc", 5, 4500000.0, 22500000.0, "Công ty CP Tin Học Sao Mai", "Đi kèm laptop"},
    //             {3, "PRN-HP-M404", "Máy in HP LaserJet Pro M404dn", "Thiết bị VP", "Chiếc", 1, 6800000.0, 6800000.0, "Công ty TNHH Thiết Bị Nam Cường", "Phòng Kế toán"}
    //         };

    //         for (int r = 0; r < samples.length; r++) {
    //             Row row = sheet.createRow(r + 1);
    //             row.setHeightInPoints(20);
    //             Object[] data = samples[r];

    //             setCell(row, 0, String.valueOf(data[0]), dataCenterStyle);
    //             setCell(row, 1, String.valueOf(data[1]), dataCenterStyle);
    //             setCell(row, 2, String.valueOf(data[2]), dataLeftStyle);
    //             setCell(row, 3, String.valueOf(data[3]), dataLeftStyle);
    //             setCell(row, 4, String.valueOf(data[4]), dataCenterStyle);

    //             Cell qCell = row.createCell(5);
    //             qCell.setCellValue(((Number) data[5]).intValue());
    //             qCell.setCellStyle(dataCenterStyle);

    //             Cell pCell = row.createCell(6);
    //             pCell.setCellValue(((Number) data[6]).doubleValue());
    //             pCell.setCellStyle(dataNumberStyle);

    //             Cell tCell = row.createCell(7);
    //             tCell.setCellValue(((Number) data[7]).doubleValue());
    //             tCell.setCellStyle(dataNumberStyle);

    //             setCell(row, 8, String.valueOf(data[8]), dataLeftStyle);
    //             setCell(row, 9, String.valueOf(data[9]), dataLeftStyle);
    //         }

    //         for (int i = 0; i < columns.length; i++) {
    //             sheet.autoSizeColumn(i);
    //             if (sheet.getColumnWidth(i) < 3200) {
    //                 sheet.setColumnWidth(i, 3800);
    //             }
    //         }

    //         workbook.write(out);
    //         return out.toByteArray();
    //     }
    // }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private int addInfoRow(Sheet sheet, int rowIdx, String label1, String value1, String label2, String value2, CellStyle labelStyle, CellStyle valueStyle) {
        Row r = sheet.createRow(rowIdx);
        Cell l1 = r.createCell(0);
        l1.setCellValue(label1);
        l1.setCellStyle(labelStyle);

        Cell v1 = r.createCell(1);
        v1.setCellValue(value1 != null ? value1 : "");
        v1.setCellStyle(valueStyle);

        Cell l2 = r.createCell(3);
        l2.setCellValue(label2);
        l2.setCellStyle(labelStyle);

        Cell v2 = r.createCell(4);
        v2.setCellValue(value2 != null ? value2 : "");
        v2.setCellStyle(valueStyle);
        return rowIdx + 1;
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
