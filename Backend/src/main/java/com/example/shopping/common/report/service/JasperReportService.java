package com.example.shopping.common.report.service;

import java.util.Collection;
import java.util.Map;

public interface JasperReportService {
    byte[] generatePdf(String templatePath, Map<String, Object> parameters, Collection<?> dataCollection) throws Exception;
}
