package com.example.shopping.requestForQuotation.repository.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.data.jpa.domain.Specification;

import com.example.shopping.requestForQuotation.dto.request.RfqSearchRequest;
import com.example.shopping.requestForQuotation.entity.Rfq;
import com.example.shopping.requestForQuotation.enums.RfqStatus;

import jakarta.persistence.criteria.Predicate;

public class RfqSpecifications {
    private RfqSpecifications() {}

    public static Specification<Rfq> search(RfqSearchRequest request) {
        return search(request, List.of(), null, null, null, false);
    }

    public static Specification<Rfq> search(
            RfqSearchRequest request,
            List<RfqStatus> statuses,
            String creatorKeyword,
            String kpiFilter,
            String currentUsername,
            boolean excludeCreator) {
        String keyword = normalize(request.keyword());
        String department = normalize(request.department());
        String createdBy = normalize(request.createdBy());
        String creator = normalize(creatorKeyword);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (statuses != null && !statuses.isEmpty())
                predicates.add(root.get("status").in(statuses));
            else if (request.status() != null)
                predicates.add(cb.equal(root.get("status"), request.status()));
            List<RfqStatus> kpiStatuses = statusesForKpi(kpiFilter);
            if (!kpiStatuses.isEmpty()) predicates.add(root.get("status").in(kpiStatuses));
            if (department != null)
                predicates.add(cb.like(cb.lower(root.get("department")),
                    "%" + department.toLowerCase(Locale.ROOT) + "%"));
            if (createdBy != null)
                predicates.add(cb.equal(root.get("createdBy"), createdBy));
            if (creator != null)
                predicates.add(cb.like(cb.lower(root.get("createdBy")),
                        "%" + creator.toLowerCase(Locale.ROOT) + "%"));
            if (excludeCreator && currentUsername != null)
                predicates.add(cb.notEqual(root.get("createdBy"), currentUsername));
            if (request.createdFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(
                    root.get("createdAt"), request.createdFrom().atStartOfDay()));
            if (request.createdTo() != null)
                predicates.add(cb.lessThan(
                    root.get("createdAt"), request.createdTo().plusDays(1).atStartOfDay()));
            if ("C_OVERDUE".equals(kpiFilter)) {
                predicates.add(cb.lessThan(root.get("createdAt"), LocalDateTime.now().minusHours(24)));
            }
            if ("A_OVERDUE".equals(kpiFilter)) {
                predicates.add(cb.lessThan(root.get("deadline"), LocalDateTime.now()));
            }
            if ("C_APPROVED".equals(kpiFilter)) {
                YearMonth currentMonth = YearMonth.now();
                predicates.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("reviewedAt"), currentMonth.atDay(1).atStartOfDay()));
                predicates.add(cb.lessThan(
                        root.<LocalDateTime>get("reviewedAt"), currentMonth.plusMonths(1).atDay(1).atStartOfDay()));
            }
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
                var ticket = root.join("procurementTicket", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("rfqCode")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(ticket.get("ticketCode")), pattern),
                        cb.like(cb.lower(ticket.get("title")), pattern)));
                query.distinct(true);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
    };
}
    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<RfqStatus> statusesForKpi(String kpi) {
        if (kpi == null) return List.of();
        return switch (kpi) {
            case "M_DRAFT" -> List.of(RfqStatus.DRAFT);
            case "M_RETURNED" -> List.of(RfqStatus.RETURNED);
            case "M_WAITING" -> List.of(RfqStatus.SENT, RfqStatus.RECEIVING_QUOTES, RfqStatus.CLOSED_QUOTES);
            case "M_COMPARE" -> List.of(RfqStatus.COMPARING, RfqStatus.PENDING_RESULT_APPROVAL);
            case "C_RFQ" -> List.of(RfqStatus.PENDING_RFQ_APPROVAL);
            case "C_RESULT" -> List.of(RfqStatus.PENDING_RESULT_APPROVAL);
            case "C_OVERDUE" -> List.of(RfqStatus.PENDING_RFQ_APPROVAL, RfqStatus.PENDING_RESULT_APPROVAL);
            case "A_PENDING" -> List.of(RfqStatus.PENDING_RFQ_APPROVAL, RfqStatus.PENDING_RESULT_APPROVAL);
            case "A_OPEN", "A_VALUE", "A_OVERDUE" -> List.of(
                    RfqStatus.DRAFT, RfqStatus.PENDING_RFQ_APPROVAL, RfqStatus.RETURNED,
                    RfqStatus.SENT, RfqStatus.RECEIVING_QUOTES, RfqStatus.CLOSED_QUOTES,
                    RfqStatus.COMPARING, RfqStatus.PENDING_RESULT_APPROVAL);
            default -> List.of();
        };
    }
}
