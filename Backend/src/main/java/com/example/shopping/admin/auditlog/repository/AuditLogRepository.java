package com.example.shopping.admin.auditlog.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.shopping.admin.auditlog.entity.AuditLogEntry;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID>, JpaSpecificationExecutor<AuditLogEntry> {

    List<AuditLogEntry> findTop10ByOrderByOccurredAtDesc();
}
