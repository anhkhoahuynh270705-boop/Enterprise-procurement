package com.example.shopping.company.repository.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;

import com.example.shopping.company.dto.CompanyDocumentSearchCriteria;
import com.example.shopping.company.entity.CompanyDocument;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public final class CompanyDocumentSpecifications {

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "POLICY", "nội quy & chính sách",
            "PROCEDURE", "quy trình",
            "TEMPLATE", "biểu mẫu",
            "TRAINING", "đào tạo",
            "OTHER", "khác");

    private CompanyDocumentSpecifications() {
    }

    public static Specification<CompanyDocument> search(CompanyDocumentSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            String category = normalize(criteria.category());
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            addContains(predicates, root, cb, "uploadedBy", criteria.uploadedBy());

            if (criteria.uploadedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("uploadedAt"), criteria.uploadedFrom().atStartOfDay()));
            }
            if (criteria.uploadedTo() != null) {
                predicates.add(cb.lessThan(
                        root.get("uploadedAt"), criteria.uploadedTo().plusDays(1).atStartOfDay()));
            }

            addFileTypeFilter(predicates, root, cb, criteria.fileType());
            addKeywordFilter(predicates, root, cb, criteria.keyword());

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addKeywordFilter(
            List<Predicate> predicates,
            Root<CompanyDocument> root,
            CriteriaBuilder cb,
            String value) {
        String keyword = normalize(value);
        if (keyword == null) 
            return;
        String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
        List<Predicate> matches = new ArrayList<>(List.of(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("filename")), pattern),
                cb.like(cb.lower(root.get("description")), pattern),
                cb.like(cb.lower(root.get("uploadedBy")), pattern),
                cb.like(cb.lower(root.get("category")), pattern)));

        CATEGORY_LABELS.forEach((category, label) -> {
            if (label.contains(keyword.toLowerCase(Locale.ROOT))) {
                matches.add(cb.equal(root.get("category"), category));
            }
        });

        predicates.add(cb.or(matches.toArray(Predicate[]::new)));
    }

    private static void addFileTypeFilter(
            List<Predicate> predicates,
            Root<CompanyDocument> root,
            CriteriaBuilder cb,
            String value) {
        String fileType = normalize(value);
        if (fileType == null) return;

        var contentType = cb.lower(root.get("contentType"));
        switch (fileType.toUpperCase(Locale.ROOT)) {
            case "PDF" -> predicates.add(cb.equal(contentType, "application/pdf"));
            case "WORD" -> predicates.add(cb.or(
                    cb.like(contentType, "%word%"),
                    cb.equal(contentType, "application/msword")));
            case "EXCEL" -> predicates.add(cb.or(
                    cb.like(contentType, "%spreadsheet%"),
                    cb.like(contentType, "%excel%")));
            case "IMAGE" -> predicates.add(cb.like(contentType, "image/%"));
            default -> {
                // Bỏ qua giá trị fileType không được hỗ trợ.
            }
        }
    }

    private static void addContains(
            List<Predicate> predicates,
            Root<CompanyDocument> root,
            CriteriaBuilder cb,
            String field,
            String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            predicates.add(cb.like(
                    cb.lower(root.get(field)),
                    "%" + normalized.toLowerCase(Locale.ROOT) + "%"));
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
