package com.example.shopping.supplier.repository.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.example.shopping.supplier.dto.request.SupplierSearchCriteria;
import com.example.shopping.supplier.entity.SupplierEntity;

import jakarta.persistence.criteria.Predicate;

public final class SupplierSpecifications {

    private SupplierSpecifications() {}

    public static Specification<SupplierEntity> search(SupplierSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            if (criteria.supplierId() != null) {
                predicates.add(cb.equal(root.get("id"), criteria.supplierId()));
            }

            addContains(predicates, root, cb, "code", criteria.supplierCode());
            addContains(predicates, root, cb, "contactPerson", criteria.contactPerson());
            addContains(predicates, root, cb, "taxCode", criteria.taxCode());
            addContains(predicates, root, cb, "email", criteria.email());
            addContains(predicates, root, cb, "phone", criteria.phone());

            String city = normalize(criteria.city());
            if (city != null) {
                predicates.add(cb.equal(
                        cb.lower(root.get("city")),
                        city.toLowerCase(Locale.ROOT)));
            }

            String keyword = normalize(criteria.keyword());
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("contactPerson")), pattern),
                        cb.like(cb.lower(root.get("taxCode")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern),
                        cb.like(cb.lower(root.get("address")), pattern),
                        cb.like(cb.lower(root.get("city")), pattern),
                        cb.like(cb.lower(root.get("country")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addContains(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<SupplierEntity> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
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
