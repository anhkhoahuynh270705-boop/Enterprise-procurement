package com.example.shopping.common.dto.response;

public record ReportFileResponse(String filename, String contentType, byte[] bytes) { }
