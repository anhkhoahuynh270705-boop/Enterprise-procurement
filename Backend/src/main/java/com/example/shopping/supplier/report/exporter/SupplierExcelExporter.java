package com.example.shopping.supplier.report.exporter;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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

import com.example.shopping.supplier.entity.SupplierEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SupplierExcelExporter {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String getContentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    public String getFileExtension() {
        return "xlsx";
    }

    /**
     * Xuất danh sách toàn bộ nhà cung cấp ra Excel
     */
    public byte[] exportSuppliers(List<SupplierEntity> suppliers) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Danh sách nhà cung cấp");
            sheet.setDisplayGridlines(true);

            // Title Font & Style
            Font titleFont = workbook.createFont();
            titleFont.setFontName("Calibri");
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setBold(true);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setFontName("Calibri");
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

            // Cell Styles
            CellStyle centerStyle = createBorderedCellStyle(workbook, HorizontalAlignment.CENTER);
            CellStyle leftStyle = createBorderedCellStyle(workbook, HorizontalAlignment.LEFT);

            // Row 1: Title
            Row titleRow = sheet.createRow(1);
            titleRow.setHeightInPoints(30);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("DANH SÁCH NHÀ CUNG CẤP & ĐỐI TÁC");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 12));

            // Row 3: Headers
            String[] columns = {
                "STT", "Mã NCC", "Tên nhà cung cấp", "Người liên hệ", "Email",
                "Số điện thoại", "Mã số thuế", "Địa chỉ", "Thành phố",
                "Ngân hàng", "Số tài khoản", "Trạng thái", "Ngày tạo"
            };

            Row headerRow = sheet.createRow(3);
            headerRow.setHeightInPoints(25);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 4;
            int stt = 1;
            for (SupplierEntity s : suppliers) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);

                createCell(row, 0, String.valueOf(stt++), centerStyle);
                createCell(row, 1, s.getCode(), centerStyle);
                createCell(row, 2, s.getName(), leftStyle);
                createCell(row, 3, s.getContactPerson(), leftStyle);
                createCell(row, 4, s.getEmail(), leftStyle);
                createCell(row, 5, s.getPhone(), centerStyle);
                createCell(row, 6, s.getTaxCode(), centerStyle);
                createCell(row, 7, s.getAddress(), leftStyle);
                createCell(row, 8, s.getCity(), leftStyle);
                createCell(row, 9, s.getBankName(), leftStyle);
                createCell(row, 10, s.getBankAccount(), centerStyle);
                createCell(row, 11, s.getStatus() != null ? s.getStatus().name() : "", centerStyle);
                createCell(row, 12, s.getCreatedAt() != null ? s.getCreatedAt().format(dateFormatter) : "", centerStyle);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Xuất chi tiết hồ sơ 1 nhà cung cấp
     */
    public byte[] exportSupplier(SupplierEntity supplier) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Hồ sơ " + supplier.getCode());
            sheet.setDisplayGridlines(true);

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
            labelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            labelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            labelStyle.setBorderTop(BorderStyle.THIN);
            labelStyle.setBorderBottom(BorderStyle.THIN);
            labelStyle.setBorderLeft(BorderStyle.THIN);
            labelStyle.setBorderRight(BorderStyle.THIN);

            CellStyle valueStyle = createBorderedCellStyle(workbook, HorizontalAlignment.LEFT);

            // Title
            Row titleRow = sheet.createRow(1);
            titleRow.setHeightInPoints(28);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("HỒ SƠ THÔNG TIN NHÀ CUNG CẤP");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

            // Info rows
            int r = 3;
            addInfoRow(sheet, r++, "Mã nhà cung cấp:", supplier.getCode(), "Trạng thái:", supplier.getStatus() != null ? supplier.getStatus().name() : "", labelStyle, valueStyle);
            addInfoRow(sheet, r++, "Tên nhà cung cấp:", supplier.getName(), "Mã số thuế:", supplier.getTaxCode(), labelStyle, valueStyle);
            addInfoRow(sheet, r++, "Người đại diện:", supplier.getContactPerson(), "Số điện thoại:", supplier.getPhone(), labelStyle, valueStyle);
            addInfoRow(sheet, r++, "Địa chỉ Email:", supplier.getEmail(), "Tỉnh / Thành phố:", supplier.getCity(), labelStyle, valueStyle);
            addInfoRow(sheet, r++, "Địa chỉ trụ sở:", supplier.getAddress(), "Quốc gia:", supplier.getCountry(), labelStyle, valueStyle);
            addInfoRow(sheet, r++, "Tên ngân hàng:", supplier.getBankName(), "Số tài khoản:", supplier.getBankAccount(), labelStyle, valueStyle);
            addInfoRow(sheet, r++, "Ngày tạo:", supplier.getCreatedAt() != null ? supplier.getCreatedAt().format(dateFormatter) : "", "Cập nhật lần cuối:", supplier.getUpdatedAt() != null ? supplier.getUpdatedAt().format(dateFormatter) : "", labelStyle, valueStyle);
            addInfoRow(sheet, r++, "Ghi chú:", supplier.getNotes() != null ? supplier.getNotes() : "", "", "", labelStyle, valueStyle);

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            sheet.autoSizeColumn(3);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * File mẫu Excel để import Nhà cung cấp
     */
    public byte[] generateSampleTemplate() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Mau_Nhap_Nha_Cung_Cap");

            Font headerFont = workbook.createFont();
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

            Row header = sheet.createRow(0);
            header.setHeightInPoints(25);
            String[] columns = {
                "Tên nhà cung cấp (*)", "Người liên hệ", "Email", "Số điện thoại",
                "Mã số thuế", "Địa chỉ", "Thành phố", "Quốc gia", "Ngân hàng", "Số tài khoản", "Ghi chú"
            };
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample row
            Row sample = sheet.createRow(1);
            CellStyle borderStyle = createBorderedCellStyle(workbook, HorizontalAlignment.LEFT);
            String[] sampleData = {
                "Công ty TNHH Thiết Bị Văn Phòng Sao Mai", "Trần Văn B", "saomai@contact.vn", "0912345678",
                "0109988776", "Tầng 5, Tòa nhà Landmark", "Hà Nội", "Việt Nam", "Vietcombank", "0011002345678", "Đối tác thiết bị văn phòng"
            };
            for (int i = 0; i < sampleData.length; i++) {
                Cell cell = sample.createCell(i);
                cell.setCellValue(sampleData[i]);
                cell.setCellStyle(borderStyle);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void addInfoRow(Sheet sheet, int r, String l1, String v1, String l2, String v2, CellStyle labelStyle, CellStyle valStyle) {
        Row row = sheet.createRow(r);
        row.setHeightInPoints(22);

        createCell(row, 0, l1, labelStyle);
        createCell(row, 1, v1 != null ? v1 : "", valStyle);
        createCell(row, 2, l2, labelStyle);
        createCell(row, 3, v2 != null ? v2 : "", valStyle);
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private CellStyle createBorderedCellStyle(Workbook workbook, HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Calibri");
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
