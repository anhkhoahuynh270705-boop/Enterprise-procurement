package com.example.shopping.requestForQuotation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopping.admin.auditlog.annotation.Audited;
import com.example.shopping.common.dto.response.ApiResponse;
import com.example.shopping.requestForQuotation.dto.request.supplierrequest.ResolveSupplierInvitationRequest;
import com.example.shopping.requestForQuotation.dto.request.supplierrequest.SupplierQuoteSubmissionRequest;
import com.example.shopping.requestForQuotation.dto.response.InvitationIssuedResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;
import com.example.shopping.requestForQuotation.dto.response.supplierresponse.SupplierInvitationViewResponse;
import com.example.shopping.requestForQuotation.service.SupplierInvitationService;
import com.example.shopping.security.TicketAccess;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class RfqInvitationController {

    SupplierInvitationService invitationService;

    /** Maker/Admin phát hành hoặc gửi lại link mời cho một NCC đã được gắn vào RFQ. */
    @PostMapping("/rfqs/{rfqId}/invitations/{supplierId}/send-link")
    @Audited(action = "Gửi link mời báo giá", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Phát hành link mời nhà cung cấp báo giá")
    public ResponseEntity<ApiResponse<InvitationIssuedResponse>> sendInvitationLink(
            @PathVariable UUID rfqId,
            @PathVariable UUID supplierId
    ) {
        InvitationIssuedResponse response = invitationService.issueLink(
                rfqId,
                supplierId,
                TicketAccess.username(),
                TicketAccess.hasRole("ADMIN")
        );

        return ResponseEntity.ok(ApiResponse.success(response, "Đã gửi link mời báo giá."));
    }

    /** NCC dùng token trong body để xem dữ liệu RFQ được phép công khai. */
    @PostMapping("/public/rfq-invitations/resolve")
    public ResponseEntity<ApiResponse<SupplierInvitationViewResponse>> resolveInvitation(
            @Valid @RequestBody ResolveSupplierInvitationRequest request
    ) {
        SupplierInvitationViewResponse response = invitationService.resolve(request.token());
        return ResponseEntity.ok(ApiResponse.success(response, "Đã xác thực lời mời báo giá."));
    }

    /** NCC gửi báo giá; nhà cung cấp được xác định từ token, không nhận supplierId từ client. */
    @PostMapping("/public/rfq-invitations/quotes")
    @Audited(action = "Nhà cung cấp gửi báo giá", module = "RFQ", moduleLabel = "Yêu cầu báo giá",
            description = "Nhận báo giá từ nhà cung cấp")
    public ResponseEntity<ApiResponse<RfqQuoteRoundResponse>> submitQuote(
            @Valid @RequestBody SupplierQuoteSubmissionRequest request
    ) {
        RfqQuoteRoundResponse response = invitationService.submit(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã gửi báo giá thành công."));
    }
}
