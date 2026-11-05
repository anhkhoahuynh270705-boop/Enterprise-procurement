package com.example.shopping.purchaseorder.controller;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopping.common.dto.response.ApiResponse;
import com.example.shopping.purchaseorder.dto.request.CreatePurchaseOrderRequest;
import com.example.shopping.purchaseorder.dto.request.PurchaseOrderSearchCriteria;
import com.example.shopping.purchaseorder.dto.request.ReviewPurchaseOrderRequest;
import com.example.shopping.purchaseorder.dto.request.UpdatePurchaseOrderStatusRequest;
import com.example.shopping.purchaseorder.dto.response.PurchaseOrderResponse;
import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;
import com.example.shopping.purchaseorder.service.PurchaseOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PurchaseOrderController {

    PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getPurchaseOrders(
            @RequestParam(required = false) PurchaseOrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseOrderService.getPurchaseOrders(status), "Lấy danh sách đơn đặt hàng thành công"));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderResponse>>> searchPurchaseOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) String ticket,
            @RequestParam(required = false) String orderCode,
            @RequestParam(required = false) BigDecimal minimumAmount,
            @RequestParam(required = false) BigDecimal maximumAmount,
            @RequestParam(required = false) LocalDate createdFrom,
            @RequestParam(required = false) LocalDate createdTo,
            @RequestParam(required = false) LocalDate deliveryFrom,
            @RequestParam(required = false) LocalDate deliveryTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PurchaseOrderSearchCriteria criteria = new PurchaseOrderSearchCriteria(
                keyword, status, supplierId, ticket, orderCode, minimumAmount, maximumAmount,
                createdFrom, createdTo, deliveryFrom, deliveryTo);
        Page<PurchaseOrderResponse> result = purchaseOrderService.searchPurchaseOrders(
                criteria,
                PageRequest.of(
                        Math.max(page, 0),
                        Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")
                                .and(Sort.by(Sort.Direction.DESC, "id"))));
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách đơn đặt hàng thành công"));
    }

    @GetMapping("/ordered-items")
    public ResponseEntity<ApiResponse<List<UUID>>> getOrderedItemIds(@RequestParam UUID ticketId) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseOrderService.getOrderedProcurementItemIds(ticketId),
                "Lấy danh sách hạng mục đã đặt thành công"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                purchaseOrderService.createPurchaseOrder(request), "Tạo đơn đặt hàng thành công"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePurchaseOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseOrderService.updateStatus(id, request.getStatus()),
                "Cập nhật trạng thái đơn đặt hàng thành công"));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> review(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewPurchaseOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                purchaseOrderService.review(id, request),
                Boolean.TRUE.equals(request.approved())
                        ? "Đã duyệt đơn đặt hàng thành công"
                        : "Đã từ chối đơn đặt hàng"));
    }
}
