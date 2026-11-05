package com.example.shopping.supplier.document.repository.specification;

import com.example.shopping.supplier.document.dto.request.SupplierDocumentSearchCriteria;
import com.example.shopping.supplier.document.entity.SupplierDocument;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class SupplierDocumentSpecifications {
    private SupplierDocumentSpecifications() {
    }

    public static Specification<SupplierDocument> search(
            SupplierDocumentSearchCriteria criteria,
            boolean reviewer,
            String username) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!reviewer) {
                predicates.add(cb.or(
                        cb.equal(root.get("status"), com.example.shopping.supplier.document.enums.SupplierDocumentStatus.APPROVED),
                        cb.equal(root.get("submittedBy"), username)));
            }
            if (criteria.supplierId() != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), criteria.supplierId()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            String keyword = normalize(criteria.keyword());
            if (keyword != null) {
                String pattern = pattern(keyword);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("filename")), pattern),
                        cb.like(cb.lower(root.get("supplier").get("name")), pattern),
                        cb.like(cb.lower(root.get("submittedBy")), pattern),
                        cb.like(cb.lower(root.get("reviewedBy")), pattern),
                        cb.like(cb.lower(root.get("reviewComment")), pattern)));
            }

            addTextFilter(predicates, cb, root.get("submittedBy"), criteria.submittedBy());
            addTextFilter(predicates, cb, root.get("reviewedBy"), criteria.reviewedBy());
            addTextFilter(predicates, cb, root.get("reviewComment"), criteria.reviewComment());
            addDateRange(predicates, cb, root.get("submittedAt"), criteria.submittedFrom(), criteria.submittedTo());
            addDateRange(predicates, cb, root.get("reviewedAt"), criteria.reviewedFrom(), criteria.reviewedTo());

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addTextFilter(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Path<String> field,
            String value) {
        String normalized = normalize(value);
        if (normalized != null) predicates.add(cb.like(cb.lower(field), pattern(normalized)));
    }

    private static void addDateRange(
            List<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Path<java.time.LocalDateTime> field,
            LocalDate from,
            LocalDate to) {
        if (from != null) predicates.add(cb.greaterThanOrEqualTo(field, from.atStartOfDay()));
        if (to != null) predicates.add(cb.lessThan(field, to.plusDays(1).atStartOfDay()));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String pattern(String value) {
        return "%" + value + "%";
    }
}
