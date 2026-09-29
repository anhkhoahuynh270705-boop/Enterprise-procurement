package com.example.shopping.common.report.service.impl;

import com.example.shopping.common.report.service.JasperReportService;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Slf4j
@Service
public class JasperReportServiceImpl implements JasperReportService {

    @Override
    public byte[] generatePdf(String templatePath, Map<String, Object> parameters, Collection<?> dataCollection) throws Exception {
        log.info("Khởi tạo báo cáo JasperReports với template: {}", templatePath);

        InputStream reportStream = getClass().getResourceAsStream(templatePath);
        if (reportStream == null) {
            log.error("Không tìm thấy file template: {}", templatePath);
            throw new IllegalStateException("Không tìm thấy template JasperReports: " + templatePath);
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        JRDataSource dataSource = (dataCollection != null && !dataCollection.isEmpty())
                ? new JRBeanCollectionDataSource(dataCollection)
                : new JREmptyDataSource();

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}
