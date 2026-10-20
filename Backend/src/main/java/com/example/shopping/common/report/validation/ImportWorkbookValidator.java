package com.example.shopping.common.report.validation;

import com.example.shopping.common.enums.ErrorEnum;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public final class ImportWorkbookValidator {
    private ImportWorkbookValidator() {}

    public record CellPosition(int rowIndex, int columnIndex) {}

    public record SupplierHeaders(
            int rowIndex,
            Map<String, Integer> columns,
            Map<String, CellPosition> profileValues) {
        public boolean isProfile() {
            return !profileValues.isEmpty();
        }
    }

    public static void validateUpload(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || filename == null || !filename.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$")) {
            throw ErrorEnum.EXCEL_INVALID_FILE.ex();
        }
    }

    public static Workbook open(InputStream stream) {
        try {
            Workbook workbook = WorkbookFactory.create(stream);
            if (workbook.getNumberOfSheets() == 0) {
                workbook.close();
                throw ErrorEnum.EXCEL_NO_SHEET.ex();
            }
            return workbook;
        } catch (Exception ex) {
            throw ErrorEnum.EXCEL_UNREADABLE.ex(ex);
        }
    }

    // CHECK VALID SUPPLIER TEMPLATE
    public static SupplierHeaders validateSupplierHeaders(Sheet sheet) {
        String[] expected = { "Tên nhà cung cấp", "Người liên hệ", "Email", "Số điện thoại",
                "Mã số thuế", "Địa chỉ", "Thành phố", "Quốc gia", "Ngân hàng", "Số tài khoản", "Ghi chú" };
        Map<String, String> aliases = new HashMap<>();
        for (String column : expected) {
            aliases.put(normalize(column), column);
        }
        aliases.put(normalize("Mã NCC"), "Mã NCC");
        aliases.put(normalize("STT"), "STT");
        aliases.put(normalize("Trạng thái"), "Trạng thái");
        aliases.put(normalize("Ngày tạo"), "Ngày tạo");

        DataFormatter formatter = new DataFormatter();
        for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), 20); rowIndex++) {
            Row header = sheet.getRow(rowIndex);
            if (header == null) {
                continue;
            }

            Map<String, Integer> columns = new HashMap<>();
            for (Cell cell : header) {
                String headerValue = normalize(formatter.formatCellValue(cell));
                String column = aliases.get(headerValue);
                if (column != null && columns.putIfAbsent(column, cell.getColumnIndex()) != null) {
                    throw invalidSupplier();
                }
            }
            if (columns.containsKey("Tên nhà cung cấp") && columns.containsKey("Mã số thuế")) {
                return new SupplierHeaders(rowIndex, columns, Map.of());
            }
        }

        Map<String, String> profileLabels = new HashMap<>();
        profileLabels.put(normalize("Tên nhà cung cấp:"), "Tên nhà cung cấp");
        profileLabels.put(normalize("Mã số thuế:"), "Mã số thuế");
        profileLabels.put(normalize("Người đại diện:"), "Người liên hệ");
        profileLabels.put(normalize("Địa chỉ Email:"), "Email");
        profileLabels.put(normalize("Số điện thoại:"), "Số điện thoại");
        profileLabels.put(normalize("Tỉnh / Thành phố:"), "Thành phố");
        profileLabels.put(normalize("Địa chỉ trụ sở:"), "Địa chỉ");
        profileLabels.put(normalize("Quốc gia:"), "Quốc gia");
        profileLabels.put(normalize("Tên ngân hàng:"), "Ngân hàng");
        profileLabels.put(normalize("Số tài khoản:"), "Số tài khoản");
        profileLabels.put(normalize("Ghi chú:"), "Ghi chú");

        Map<String, CellPosition> profileValues = new HashMap<>();
        for (int rowIndex = 0; rowIndex <= Math.min(sheet.getLastRowNum(), 30); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            for (Cell labelCell : row) {
                String field = profileLabels.get(normalize(new DataFormatter().formatCellValue(labelCell)));
                if (field != null && labelCell.getColumnIndex() + 1 < row.getLastCellNum()) {
                    profileValues.put(field, new CellPosition(rowIndex, labelCell.getColumnIndex() + 1));
                }
            }
        }
        if (profileValues.containsKey("Tên nhà cung cấp") && profileValues.containsKey("Mã số thuế")) {
            return new SupplierHeaders(profileValues.get("Tên nhà cung cấp").rowIndex(), Map.of(), profileValues);
        }
        throw invalidSupplier();
    }

    // CHECK VALID PROCUREMENT TEMPLATE 
    public static void validateProcurementHeaders(Sheet sheet, String[] expectedHeaders) {
        Row header = sheet.getRow(0);
        DataFormatter formatter = new DataFormatter();
        if (header == null || expectedHeaders == null || expectedHeaders.length == 0) {
            throw ErrorEnum.EXCEL_MISSING_TEMPLATE_HEADERS.ex();
        }

        for (int column = 0; column < expectedHeaders.length; column++) {
            String actual = normalize(formatter.formatCellValue(header.getCell(column)));
            if (!normalize(expectedHeaders[column]).equals(actual)) {
                throw ErrorEnum.EXCEL_MISSING_TEMPLATE_HEADERS.ex();
            }
        }

        for (int column = expectedHeaders.length; column < header.getLastCellNum(); column++) {
            if (!formatter.formatCellValue(header.getCell(column)).isBlank()) {
                throw ErrorEnum.EXCEL_MISSING_TEMPLATE_HEADERS.ex();
            }
        }
    }

    private static ResponseStatusException invalidSupplier() {
        return ErrorEnum.EXCEL_INVALID_SUPPLIER_TEMPLATE.ex();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').replaceAll("[()*]", "")
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
