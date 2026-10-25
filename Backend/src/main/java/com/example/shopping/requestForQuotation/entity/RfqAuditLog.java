package com.example.shopping.requestForQuotation.entity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "rfq_audit_logs",
        indexes = @Index(name = "idx_rfq_audit_logs_rfq_created", columnList = "rfq_id, created_at")
)
@Getter
@NoArgsConstructor
public class RfqAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(nullable = false, length = 100)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RfqAuditLog(Rfq rfq, String actorUsername, String action,
                       Map<String, Object> details, LocalDateTime createdAt) {
        this.rfq = rfq;
        this.actorUsername = actorUsername;
        this.action = action;
        this.details = details;
        this.createdAt = createdAt;
    }
}
