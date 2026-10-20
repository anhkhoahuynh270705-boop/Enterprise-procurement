package com.example.shopping.admin.auditlog.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.shopping.admin.auditlog.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.admin.auditlog.dto.AuditLogDto;
import com.example.shopping.admin.auditlog.entity.AuditLogEntry;
import com.example.shopping.admin.auditlog.mapper.AuditLogMapper;
import com.example.shopping.admin.auditlog.repository.AuditLogRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    AuditLogRepository repo;
    AuditLogMapper mapper;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(
            String module,
            String status,
            String dateRange,
            String keyword,
            int page,
            int size
    ) {
        LocalDateTime from = switch (dateRange == null ? "TODAY" : dateRange.toUpperCase()) {
            case "WEEK" -> LocalDateTime.now().minusDays(7);
            case "MONTH" -> LocalDateTime.now().minusDays(30);
            case "ALL" -> null;
            default -> LocalDateTime.now().toLocalDate().atStartOfDay();
        };

        Specification<AuditLogEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (module != null && !"ALL".equalsIgnoreCase(module) && !module.isBlank()) {
                predicates.add(cb.equal(root.get("module"), module));
            }

            if (status != null && !"ALL".equalsIgnoreCase(status) && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("logCode")), pattern),
                        cb.like(cb.lower(root.get("action")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"));
        return repo.findAll(spec, pageRequest).map(mapper::toDto);
    }

    @Transactional
    public void record(AuditLogEntry entry) {
        repo.save(entry);
    }

}
