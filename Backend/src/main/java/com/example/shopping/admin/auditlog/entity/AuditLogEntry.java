package com.example.shopping.admin.auditlog.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor
@Builder
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "log_code", length = 20, nullable = false)
    private String logCode;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "user_full_name", length = 255)
    private String userFullName;

    @Column(name = "action", length = 500, nullable = false)
    private String action;

    @Column(name = "module", length = 50, nullable = false)
    private String module;

    @Column(name = "module_label", length = 100)
    private String moduleLabel;

    /** SUCCESS | WARNING | FAILED */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "payload_summary", columnDefinition = "TEXT")
    private String payloadSummary;
}
