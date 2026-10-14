package com.example.shopping.procurement.controller;

import java.util.List;
import java.util.UUID;

import com.example.shopping.admin.auditlog.annotation.Audited;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopping.common.dto.response.ApiResponse;
import com.example.shopping.procurement.dto.request.CreateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.request.UpdateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.service.ProcurementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProcurementController {

    ProcurementService procurementService;

    private String extractUsername(Jwt jwt) {
        if (jwt == null) {
            return "system_maker";
        }
        String preferred = jwt.getClaimAsString("preferred_username");
        return preferred != null ? preferred : jwt.getSubject();
    }

    /**
     * Maker tạo phiếu mua sắm mới
     */
    @PostMapping("/tickets")
    @Audited(
        action = "Tạo phiếu mua sắm",
        module = "PROCUREMENT",
        moduleLabel = "Mua sắm",
        description = "Maker tạo mới phiếu mua sắm"
    )
    public ResponseEntity<ApiResponse<ProcurementTicketResponseDto>> createTicket(
            @Valid @RequestBody CreateProcurementTicketRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = extractUsername(jwt);
        ProcurementTicketResponseDto response = procurementService.createTicket(request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tạo phiếu mua sắm thành công"));
    }

    /**
     * Lấy danh sách phiếu mua sắm
     */
    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<ProcurementTicketResponseDto>>> getAllTickets(
            @RequestParam(required = false) ProcurementStatus status,
            @RequestParam(required = false) String maker) {
        List<ProcurementTicketResponseDto> list = procurementService.getAllTickets(status, maker);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách phiếu thành công"));
    }

    @GetMapping("/tickets/page")
    public ResponseEntity<ApiResponse<Page<ProcurementTicketResponseDto>>> getTicketPage(
            @RequestParam(required = false) ProcurementStatus status,
            @RequestParam(required = false) String maker,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<ProcurementTicketResponseDto> result = procurementService.getTicketPage(
                status,
                maker,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách phiếu thành công"));
    }

    /**
     * Lấy chi tiết một phiếu mua sắm
     */
    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<ProcurementTicketResponseDto>> getTicketById(@PathVariable UUID id) {
        ProcurementTicketResponseDto dto = procurementService.getTicketById(id);
        return ResponseEntity.ok(ApiResponse.success(dto, "Lấy chi tiết phiếu thành công"));
    }

    /**
     * Cập nhật thông tin phiếu
     */
    @PutMapping("/tickets/{id}")
    @Audited(
        action = "Cập nhật phiếu mua sắm",
        module = "PROCUREMENT",
        moduleLabel = "Mua sắm",
        description = "Chỉnh sửa thông tin phiếu mua sắm"
    )
    public ResponseEntity<ApiResponse<ProcurementTicketResponseDto>> updateTicket(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProcurementTicketRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        String username = extractUsername(jwt);
        ProcurementTicketResponseDto dto = procurementService.updateTicket(id, request, username);
        return ResponseEntity.ok(ApiResponse.success(dto, "Cập nhật phiếu thành công"));
    }

    /**
     * Xóa phiếu mua sắm
     */
    @DeleteMapping("/tickets/{id}")
    @Audited(
        action = "Xóa phiếu mua sắm",
        module = "PROCUREMENT",
        moduleLabel = "Mua sắm",
        description = "Hủy bỏ / xóa phiếu mua sắm"
    )
    public ResponseEntity<ApiResponse<Void>> deleteTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = extractUsername(jwt);
        procurementService.deleteTicket(id, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa phiếu mua sắm thành công"));
    }

    /**
     * Maker nộp phiếu mua sắm để Checker duyệt
     */
    @PostMapping("/tickets/{id}/submit")
    @Audited(
        action = "Nộp phiếu mua sắm",
        module = "PROCUREMENT",
        moduleLabel = "Mua sắm",
        description = "Maker nộp phiếu mua sắm để Checker duyệt"
    )
    public ResponseEntity<ApiResponse<ProcurementTicketResponseDto>> submitTicket(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String username = extractUsername(jwt);
        ProcurementTicketResponseDto dto = procurementService.submitTicket(id, username);
        return ResponseEntity.ok(ApiResponse.success(dto, "Nộp phiếu mua sắm lên Checker thành công"));
    }
}
