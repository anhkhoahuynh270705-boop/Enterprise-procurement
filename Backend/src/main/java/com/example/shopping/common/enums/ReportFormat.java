package com.example.shopping.common.enums;

import lombok.Getter;

@Getter
public enum ReportFormat {
    PDF("application/pdf", "pdf"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String contentType;
    private final String extension;

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
