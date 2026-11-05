package com.example.shopping.supplier.proposal.repository.specification;

import com.example.shopping.supplier.proposal.dto.request.SupplierProposalSearchCriteria;
import com.example.shopping.supplier.proposal.entity.SupplierProposal;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class SupplierProposalSpecifications {
    private SupplierProposalSpecifications() {
    }

    public static Specification<SupplierProposal> search(
            SupplierProposalSearchCriteria criteria,
            boolean reviewer,
            String username) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!reviewer) predicates.add(cb.equal(root.get("submittedBy"), username));
            if (criteria.status() != null) predicates.add(cb.equal(root.get("status"), criteria.status()));

            if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("taxCode")), pattern),
                        cb.like(cb.lower(root.get("contactPerson")), pattern),
                        cb.like(cb.lower(root.get("submittedBy")), pattern),
                        cb.like(cb.lower(root.get("city")), pattern),
                        cb.like(cb.lower(root.get("reason")), pattern)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
