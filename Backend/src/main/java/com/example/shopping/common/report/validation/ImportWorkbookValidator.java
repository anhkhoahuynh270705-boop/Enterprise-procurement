package com.example.shopping.common.report.validation;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.Locale;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

public final class ImportWorkbookValidator {
    private ImportWorkbookValidator() {}

    public static void validateUpload(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || filename == null || !filename.toLowerCase(Locale.ROOT).matches(".*\\.(xlsx|xls)$")) {
            throw new IllegalArgumentException("File import không hợp lệ. Vui lòng chọn file Excel .xlsx hoặc .xls không rỗng.");
        }
    }

    public static Workbook open(InputStream stream) {
        try {
            Workbook workbook = WorkbookFactory.create(stream);
            if (workbook.getNumberOfSheets() == 0) {
                workbook.close();
                throw new IllegalArgumentException("Workbook has no sheets");
            }
            return workbook;
        } catch (Exception ex) {
            throw new IllegalArgumentException("File import không hợp lệ. File phải là Excel đọc được, có trang tính và không được khóa bằng mật khẩu.", ex);
        }
    }

    public static void validateSupplierHeaders(Sheet sheet) {
        String[] expected = { "Tên nhà cung cấp", "Người liên hệ", "Email", "Số điện thoại",
                "Mã số thuế", "Địa chỉ", "Thành phố", "Quốc gia", "Ngân hàng", "Số tài khoản", "Ghi chú" };
        Row header = sheet.getRow(0);
        DataFormatter formatter = new DataFormatter();
        for (int index = 0; index < expected.length; index++) {
            String actual = header == null ? "" : formatter.formatCellValue(header.getCell(index));
            if (!normalize(actual).equals(normalize(expected[index]))) {
                throw invalidSupplier();
            }
        }
        for (int index = expected.length; index < header.getLastCellNum(); index++) {
            if (!formatter.formatCellValue(header.getCell(index)).isBlank()) 
                throw invalidSupplier();
        }
    }

    private static IllegalArgumentException invalidSupplier() {
        return new IllegalArgumentException("File import không hợp lệ. Vui lòng dùng đúng mẫu nhà cung cấp.");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').replaceAll("[()*]", "")
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
