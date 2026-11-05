package com.example.shopping.purchaseorder.repository.specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.example.shopping.purchaseorder.dto.request.PurchaseOrderSearchCriteria;
import com.example.shopping.purchaseorder.entity.PurchaseOrder;
import com.example.shopping.purchaseorder.enums.PurchaseOrderStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public final class PurchaseOrderSpecifications {

    private PurchaseOrderSpecifications() {
    }

    public static Specification<PurchaseOrder> search(
            PurchaseOrderSearchCriteria criteria,
            String username,
            boolean canViewAll) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!canViewAll) {
                predicates.add(cb.or(
                        root.get("status").in(PurchaseOrderStatus.SENT, PurchaseOrderStatus.RECEIVED),
                        cb.and(
                                cb.equal(root.get("createdBy"), username),
                                root.get("status").in(PurchaseOrderStatus.PENDING_APPROVAL,
                                        PurchaseOrderStatus.DRAFT))));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.supplierId() != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), criteria.supplierId()));
            }
            addContains(predicates, cb, root.<String>get("orderCode"), criteria.orderCode());
            String ticket = normalize(criteria.ticket());
            if (ticket != null) {
                String ticketPattern = pattern(ticket);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("ticket").<String>get("ticketCode")), ticketPattern),
                        cb.like(cb.lower(root.get("ticket").<String>get("title")), ticketPattern)));
            }

            if (criteria.minimumAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), criteria.minimumAmount()));
            }
            if (criteria.maximumAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), criteria.maximumAmount()));
            }
            if (criteria.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("createdAt"), criteria.createdFrom().atStartOfDay()));
            }
            if (criteria.createdTo() != null) {
                predicates.add(cb.lessThan(
                        root.<LocalDateTime>get("createdAt"), criteria.createdTo().plusDays(1).atStartOfDay()));
            }
            if (criteria.deliveryFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<LocalDate>get("expectedDeliveryDate"), criteria.deliveryFrom()));
            }
            if (criteria.deliveryTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.<LocalDate>get("expectedDeliveryDate"), criteria.deliveryTo()));
            }

            String keyword = normalize(criteria.keyword());
            if (keyword != null) {
                String pattern = pattern(keyword);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("orderCode")), pattern),
                        cb.like(cb.lower(root.get("ticket").<String>get("ticketCode")), pattern),
                        cb.like(cb.lower(root.get("ticket").<String>get("title")), pattern),
                        cb.like(cb.lower(root.get("supplier").<String>get("name")), pattern),
                        cb.like(cb.lower(root.get("supplier").<String>get("code")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addContains(
            List<Predicate> predicates,
            CriteriaBuilder cb,
            Expression<String> field,
            String value) {
        String normalized = normalize(value);
        if (normalized == null) return;
        predicates.add(cb.like(cb.lower(field), pattern(normalized)));
    }

    private static String pattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
