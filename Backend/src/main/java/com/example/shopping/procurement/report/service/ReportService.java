package com.example.shopping.procurement.report.service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import com.example.shopping.common.dto.response.ReportFileResponse;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.report.dto.response.ImportResultDto;

public interface ReportService {
    ReportFileResponse exportTicket(UUID id, String format, boolean detail) throws Exception;
    ReportFileResponse exportAllTickets() throws Exception;
    ImportResultDto importTickets(MultipartFile file, String makerUsername) throws Exception;
    byte[] exportTicketPdf(ProcurementTicket ticket) throws Exception;
    byte[] exportTicketExcel(ProcurementTicket ticket) throws Exception;
    byte[] exportTicketDetailPdf(ProcurementTicket ticket) throws Exception;
    byte[] exportTicketDetailExcel(ProcurementTicket ticket) throws Exception;
    byte[] exportTicketsExcel(List<ProcurementTicket> tickets) throws Exception;
    ImportResultDto importExcelTickets(InputStream inputStream, String originalFilename, String makerUsername) throws Exception;
}
