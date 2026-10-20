package com.example.shopping.purchaseorder.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;

import com.example.shopping.common.enums.ErrorEnum;

import com.example.shopping.procurement.entity.ProcurementItem;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.enums.ProcurementStatus;
import com.example.shopping.procurement.repository.ProcurementTicketRepository;
import com.example.shopping.purchaseorder.dto.request.CreatePurchaseOrderRequest;
import com.example.shopping.purchaseorder.dto.request.ReviewPurchaseOrderRequest;
import com.example.shopping.purchaseorder.dto.response.PurchaseOrderResponse;
import com.example.shopping.purchaseorder.entity.PurchaseOrder;
import com.example.shopping.purchaseorder.entity.PurchaseOrderItem;
import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;
import com.example.shopping.purchaseorder.mapper.PurchaseOrderMapper;
import com.example.shopping.purchaseorder.repository.PurchaseOrderRepository;
import com.example.shopping.purchaseorder.service.PurchaseOrderService;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.supplier.entity.SupplierEntity;
import com.example.shopping.supplier.enums.SupplierStatus;
import com.example.shopping.supplier.repository.SupplierRepository;
import com.example.shopping.workflow.camunda.service.PurchaseOrderWorkflowService;

import lombok.RequiredArgsConstructor;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    PurchaseOrderRepository purchaseOrderRepository;
    ProcurementTicketRepository ticketRepository;
    SupplierRepository supplierRepository;
    PurchaseOrderMapper purchaseOrderMapper;
    PurchaseOrderWorkflowService workflow;
    PurchaseOrderCache purchaseOrderCache;

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getPurchaseOrders(PurchaseOrderStatus status) {
        TicketAccess.requireReader();
        return purchaseOrderCache.getPurchaseOrders(status);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = PurchaseOrderCache.CACHE_NAME, allEntries = true)
    public synchronized PurchaseOrderResponse createPurchaseOrder(CreatePurchaseOrderRequest request) {
        if (!TicketAccess.hasRole("USER") && !TicketAccess.hasRole("ADMIN")) {
            throw ErrorEnum.ACCESS_DENIED.ex(
                    "Chỉ người dùng mua sắm hoặc quản trị viên mới được tạo đơn đặt hàng.");
        }
        ProcurementTicket ticket = ticketRepository.findByIdForUpdate(request.getTicketId())
                .orElseThrow(() -> ErrorEnum.TICKET_NOT_FOUND.ex());
        TicketAccess.requireRead(ticket);
        if (ticket.getStatus() != ProcurementStatus.APPROVED) {
            throw ErrorEnum.TICKET_NOT_APPROVED.ex();
        }

        Set<UUID> requestedIds = new HashSet<>(request.getProcurementItemIds());
        if (requestedIds.size() != request.getProcurementItemIds().size()) {
            throw ErrorEnum.TICKET_ITEM_DUPLICATE.ex();
        }

        List<ProcurementItem> selectedItems = ticket.getItems().stream()
                .filter(item -> requestedIds.contains(item.getId()))
                .toList();
        if (selectedItems.size() != requestedIds.size()) {
            throw ErrorEnum.TICKET_ITEM_NOT_IN_TICKET.ex();
        }

        List<UUID> alreadyOrdered = purchaseOrderRepository
                .findAlreadyOrderedProcurementItemIds(new ArrayList<>(requestedIds));
        if (!alreadyOrdered.isEmpty()) {
            throw ErrorEnum.PURCHASE_ORDER_ITEM_DUPLICATED.ex();
        }

        SupplierEntity supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> ErrorEnum.SUPPLIER_NOT_FOUND.ex());
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw ErrorEnum.SUPPLIER_NOT_ACTIVE.ex();
        }

        PurchaseOrder order = PurchaseOrder.builder()
                .orderCode(generateNextOrderCode())
                .ticket(ticket)
                .supplier(supplier)
                .status(PurchaseOrderStatus.PENDING_APPROVAL)
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .notes(request.getNotes())
                .totalAmount(BigDecimal.ZERO)
                .createdBy(TicketAccess.username())
                .createdAt(LocalDateTime.now())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (ProcurementItem sourceItem : selectedItems) {
            BigDecimal lineTotal = sourceItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(sourceItem.getQuantity()));
            PurchaseOrderItem orderItem = PurchaseOrderItem.builder()
                    .purchaseOrder(order)
                    .procurementItem(sourceItem)
                    .itemCode(sourceItem.getItemCode())
                    .itemName(sourceItem.getItemName())
                    .category(sourceItem.getCategory())
                    .quantity(sourceItem.getQuantity())
                    .unit(sourceItem.getUnit())
                    .unitPrice(sourceItem.getUnitPrice())
                    .totalPrice(lineTotal)
                    .build();
            order.getItems().add(orderItem);
            total = total.add(lineTotal);
        }
        order.setTotalAmount(total);
        order = purchaseOrderRepository.save(order);
        workflow.startReviewProcess(order);
        return purchaseOrderMapper.toResponse(order);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = PurchaseOrderCache.CACHE_NAME, allEntries = true)
    public PurchaseOrderResponse updateStatus(UUID id, PurchaseOrderStatus status) {
        if (!TicketAccess.hasRole("ADMIN") && !TicketAccess.hasRole("CHECKER")) {
            throw ErrorEnum.ACCESS_DENIED.ex(
                    "Chỉ quản trị viên hoặc người duyệt mới được cập nhật trạng thái đơn đặt hàng.");
        }
        PurchaseOrder order = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ErrorEnum.PURCHASE_ORDER_NOT_FOUND.ex());

        boolean validTransition = switch (order.getStatus()) {
            case SENT -> status == PurchaseOrderStatus.RECEIVED || status == PurchaseOrderStatus.CANCELLED;
            case PENDING_APPROVAL, DRAFT, RECEIVED, CANCELLED, REJECTED -> false;
        };
        if (!validTransition) {
            throw ErrorEnum.PURCHASE_ORDER_INVALID_STATUS.ex(
                    "Không thể chuyển trạng thái đơn hàng từ "
                    + order.getStatus() + " sang " + status + ".");
        }
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return purchaseOrderMapper.toResponse(purchaseOrderRepository.save(order));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse review(UUID id, ReviewPurchaseOrderRequest request) {
        if (!TicketAccess.hasRole("ADMIN") && !TicketAccess.hasRole("CHECKER")) {
            throw ErrorEnum.ACCESS_DENIED.ex(
                    "Chỉ quản trị viên hoặc người duyệt được duyệt đơn đặt hàng.");
        }
        PurchaseOrder order = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ErrorEnum.PURCHASE_ORDER_NOT_FOUND.ex());
        if (order.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL
                && order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw ErrorEnum.PURCHASE_ORDER_NOT_PENDING.ex();
        }

        String reviewer = TicketAccess.username();
        if (!TicketAccess.hasRole("ADMIN") && reviewer.equals(order.getCreatedBy())) {
            throw ErrorEnum.PURCHASE_ORDER_SELF_REVIEW.ex();
        }
        workflow.completeReview(order, reviewer, request.approved(), request.comment());
        PurchaseOrder reviewedOrder = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> ErrorEnum.PURCHASE_ORDER_NOT_FOUND.ex());
        return purchaseOrderMapper.toResponse(reviewedOrder);
    }

    private String generateNextOrderCode() {
        int year = Year.now().getValue();
        String prefix = "PO-" + year + "-";
        return String.format("PO-%d-%04d", year, purchaseOrderRepository.countByOrderCodePrefix(prefix) + 1);
    }

}
