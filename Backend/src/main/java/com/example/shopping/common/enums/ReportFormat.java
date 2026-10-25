package com.example.shopping.common.enums;

import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public enum ReportFormat {
    PDF("application/pdf", "pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    String contentType;
    String extension;

    ReportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public static ReportFormat from(String format) {
        if (format != null) {
            if (format.equalsIgnoreCase("EXCEL") || format.equalsIgnoreCase("XLSX")) {
                return EXCEL;
            }
            if (format.equalsIgnoreCase("PDF")) {
                return PDF;
            }
        }
        return PDF;
    }
}
