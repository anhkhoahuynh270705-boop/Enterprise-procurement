package com.example.shopping.requestForQuotation.repository;

import com.example.shopping.requestForQuotation.entity.RfqAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RfqAuditLogRepository extends JpaRepository<RfqAuditLog, UUID> {
}
