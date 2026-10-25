package com.example.shopping.requestForQuotation.controller;

import com.example.shopping.admin.auditlog.annotation.Audited;
import com.example.shopping.common.dto.response.ApiResponse;
import com.example.shopping.requestForQuotation.dto.request.CreateRfqRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqActionRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqDecisionRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqSearchRequest;
import com.example.shopping.requestForQuotation.dto.request.RequestRfqRequoteRequest;
import com.example.shopping.requestForQuotation.dto.request.SelectRfqWinnersRequest;
import com.example.shopping.requestForQuotation.dto.request.UpdateRfqQuoteScoresRequest;
import com.example.shopping.requestForQuotation.dto.request.UpdateRfqRequest;
import com.example.shopping.requestForQuotation.dto.response.RfqDetailResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqSummaryResponse;
import com.example.shopping.requestForQuotation.enums.RfqStatus;
import com.example.shopping.requestForQuotation.report.exporter.RfqExcelExporter;
import com.example.shopping.requestForQuotation.report.exporter.RfqPdfExporter;
import com.example.shopping.requestForQuotation.service.RfqService;
import com.example.shopping.security.TicketAccess;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rfqs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RfqController {

    RfqService rfqService;
    RfqExcelExporter rfqExcelExporter;
    RfqPdfExporter rfqPdfExporter;

    @PostMapping
    @Audited(action = "Tạo RFQ", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Tạo yêu cầu báo giá")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> create(
            @Valid @RequestBody CreateRfqRequest request) {
        RfqDetailResponse response = rfqService.create(
                request, TicketAccess.username(), TicketAccess.hasRole("ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo yêu cầu báo giá thành công."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RfqSummaryResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RfqStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        boolean canViewAll = TicketAccess.hasRole("ADMIN") || TicketAccess.hasRole("CHECKER");

        RfqSearchRequest request = new RfqSearchRequest(keyword, status, department, createdBy,
                createdFrom, createdTo, Math.max(page, 0), Math.min(Math.max(size, 1), 100));

        RfqSearchRequest scopedRequest = canViewAll ? request : new RfqSearchRequest(
                request.keyword(), request.status(), request.department(), TicketAccess.username(),
                request.createdFrom(), request.createdTo(), request.page(), request.size());

        return ResponseEntity.ok(ApiResponse.success(
                rfqService.search(scopedRequest), "Lấy danh sách yêu cầu báo giá thành công."));
    }

    @GetMapping(value = "/export", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<RfqStatus> statuses,
            @RequestParam(required = false) RfqStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo) {
        boolean canViewAll = TicketAccess.hasRole("ADMIN") || TicketAccess.hasRole("CHECKER");
        String scopedCreator = canViewAll ? createdBy : TicketAccess.username();
        List<RfqStatus> statusFilters = statuses == null || statuses.isEmpty()
                ? (status == null ? new ArrayList<>() : new ArrayList<>(List.of(status)))
                : statuses.stream().distinct().toList();
        if (statusFilters.isEmpty()) statusFilters.add(null);

        Map<UUID, RfqSummaryResponse> uniqueRfqs = new LinkedHashMap<>();
        for (RfqStatus statusFilter : statusFilters) {
            int pageNumber = 0;
            Page<RfqSummaryResponse> page;
            do {
                RfqSearchRequest request = new RfqSearchRequest(keyword, statusFilter, department,
                        scopedCreator, createdFrom, createdTo, pageNumber, 100);
                page = rfqService.search(request);
                page.getContent().forEach(rfq -> uniqueRfqs.putIfAbsent(rfq.id(), rfq));
                pageNumber++;
            } while (page.hasNext());
        }

        String filterSummary = String.join(" · ", List.of(
                keyword == null || keyword.isBlank() ? "" : "Từ khóa: " + keyword.trim(),
                department == null || department.isBlank() ? "" : "Phòng ban: " + department.trim(),
                scopedCreator == null || scopedCreator.isBlank() ? "" : "Người tạo: " + scopedCreator.trim(),
                createdFrom == null ? "" : "Từ ngày: " + createdFrom,
                createdTo == null ? "" : "Đến ngày: " + createdTo,
                statusFilters.stream().filter(item -> item != null).map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("")
        ).stream().filter(item -> !item.isBlank()).toList());

        byte[] workbook = rfqExcelExporter.exportList(new ArrayList<>(uniqueRfqs.values()), filterSummary);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(RfqExcelExporter.CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rfq-danh-sach.xlsx\"")
                .body(workbook);
    }

    @GetMapping(value = "/{rfqId}/export/comparison", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportComparison(@PathVariable UUID rfqId) {
        boolean canViewAll = TicketAccess.hasRole("ADMIN") || TicketAccess.hasRole("CHECKER");
        RfqDetailResponse rfq = rfqService.getById(rfqId, TicketAccess.username(), canViewAll);
        byte[] workbook = rfqExcelExporter.exportComparison(rfq);
        String filename = (rfq.rfqCode() == null ? "rfq" : rfq.rfqCode()) + "-so-sanh-bao-gia.xlsx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(RfqExcelExporter.CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(workbook);
    }

    @GetMapping(value = "/{rfqId}/export/comparison/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportComparisonPdf(@PathVariable UUID rfqId) throws Exception {
        boolean canViewAll = TicketAccess.hasRole("ADMIN") || TicketAccess.hasRole("CHECKER");
        RfqDetailResponse rfq = rfqService.getById(rfqId, TicketAccess.username(), canViewAll);
        byte[] pdf = rfqPdfExporter.exportComparison(rfq);
        String filename = (rfq.rfqCode() == null ? "rfq" : rfq.rfqCode()) + "-so-sanh-bao-gia.pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    @GetMapping("/{rfqId}")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> getById(
            @PathVariable UUID rfqId) {
        boolean canViewAll = TicketAccess.hasRole("ADMIN") || TicketAccess.hasRole("CHECKER");
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.getById(rfqId, TicketAccess.username(), canViewAll),
                "Lấy chi tiết yêu cầu báo giá thành công."));
    }

    @PutMapping("/{rfqId}")
    @Audited(action = "Cập nhật RFQ", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Cập nhật thông tin yêu cầu báo giá")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> update(
            @PathVariable UUID rfqId,
            @Valid @RequestBody UpdateRfqRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.update(rfqId, request, TicketAccess.username(), TicketAccess.hasRole("ADMIN")),
                "Cập nhật yêu cầu báo giá thành công."));
    }

    @PostMapping("/{rfqId}/submit")
    @Audited(action = "Trình duyệt RFQ", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Trình duyệt yêu cầu báo giá")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> submit(
            @PathVariable UUID rfqId) {
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.submit(rfqId, TicketAccess.username(), TicketAccess.hasRole("ADMIN")),
                "Đã trình duyệt yêu cầu báo giá."));
    }

    @PostMapping("/{rfqId}/decision")
    @Audited(action = "Duyệt RFQ", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Ghi nhận quyết định duyệt RFQ")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> decide(
            @PathVariable UUID rfqId,
            @Valid @RequestBody RfqDecisionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.decide(rfqId, request, TicketAccess.username()),
                "Đã ghi nhận quyết định duyệt yêu cầu báo giá."));
    }

    @PostMapping("/{rfqId}/cancel")
    @Audited(action = "Hủy RFQ", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Hủy yêu cầu báo giá")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> cancel(
            @PathVariable UUID rfqId,
            @Valid @RequestBody RfqActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.cancel(rfqId, request, TicketAccess.username(), TicketAccess.hasRole("ADMIN")),
                "Đã hủy yêu cầu báo giá."));
    }

    @PostMapping("/{rfqId}/close-quotes")
    @Audited(action = "Đóng nhận báo giá", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Đóng nhận báo giá cho RFQ")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> closeQuotes(
            @PathVariable UUID rfqId,
            @Valid @RequestBody RfqActionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.closeQuotes(rfqId, request, TicketAccess.username(), TicketAccess.hasRole("ADMIN")),
                "Đã đóng nhận báo giá."));
    }

    @PostMapping("/{rfqId}/comparison/start")
    @Audited(action = "Bắt đầu so sánh báo giá", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Bắt đầu so sánh báo giá RFQ")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> startComparison(
            @PathVariable UUID rfqId) {
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.startComparison(rfqId, TicketAccess.username(), TicketAccess.hasRole("ADMIN")),
                "Đã bắt đầu so sánh báo giá."));
    }

    @PostMapping("/{rfqId}/requote")
    @Audited(action = "Yêu cầu chào giá lại", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Yêu cầu nhà cung cấp chào giá lại")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> requestRequote(
            @PathVariable UUID rfqId,
            @Valid @RequestBody RequestRfqRequoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.requestRequote(rfqId, request, TicketAccess.username()),
                "Đã yêu cầu nhà cung cấp chào giá lại."));
    }

    @PutMapping("/{rfqId}/quotes/{quoteRoundId}/scores")
    @Audited(action = "Cập nhật điểm báo giá", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Cập nhật điểm đánh giá báo giá nhà cung cấp")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> updateQuoteScores(
            @PathVariable UUID rfqId,
            @PathVariable UUID quoteRoundId,
            @Valid @RequestBody UpdateRfqQuoteScoresRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.updateQuoteScores(rfqId, quoteRoundId, request, TicketAccess.username()),
                "Đã cập nhật điểm đánh giá báo giá."));
    }

    @PostMapping("/{rfqId}/winners")
    @Audited(action = "Chọn nhà cung cấp", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Chọn hoặc trình duyệt nhà cung cấp thắng RFQ")
    public ResponseEntity<ApiResponse<RfqDetailResponse>> selectWinners(
            @PathVariable UUID rfqId,
            @Valid @RequestBody SelectRfqWinnersRequest request) {
        String message = request.submitForApproval()
                ? "Đã trình duyệt kết quả lựa chọn nhà cung cấp."
                : "Đã cập nhật nhà cung cấp được lựa chọn.";
        return ResponseEntity.ok(ApiResponse.success(
                rfqService.selectWinners(rfqId, request, TicketAccess.username()), message));
    }

}
