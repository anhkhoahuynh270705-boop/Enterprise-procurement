package com.example.shopping.requestForQuotation.entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.requestForQuotation.enums.RfqStatus;
import com.example.shopping.supplier.entity.SupplierEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rfqs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rfq {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rfq_code", nullable = false, unique = true, length = 50)
    private String rfqCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procurement_ticket_id", nullable = false)
    private ProcurementTicket procurementTicket;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RfqStatus status = RfqStatus.DRAFT;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "quote_validity_days", nullable = false)
    private Integer quoteValidityDays;

    @Column(name = "delivery_location", length = 500)
    private String deliveryLocation;

    @Column(name = "delivery_window", length = 255)
    private String deliveryWindow;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "estimated_value", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal estimatedValue = BigDecimal.ZERO;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_reason", length = 2000)
    private String reviewReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_supplier_id")
    private SupplierEntity selectedSupplier;

    @Column(name = "selection_reason", length = 2000)
    private String selectionReason;

    @Column(name = "exception_reason", length = 2000)
    private String exceptionReason;

    @OneToMany(mappedBy = "rfq", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<RfqItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "rfq", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<RfqInvitedSupplier> invitedSuppliers = new ArrayList<>();

    @OneToMany(mappedBy = "rfq", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<RfqQuoteRound> quoteRounds = new ArrayList<>();

    @OneToMany(mappedBy = "rfq", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @Builder.Default
    private List<RfqScoreCriterion> scoreCriteria = new ArrayList<>();

    @OneToMany(mappedBy = "rfq", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<RfqItemAward> itemAwards = new ArrayList<>();

    @OneToMany(mappedBy = "rfq")
    @Builder.Default
    private List<RfqAuditLog> auditLogs = new ArrayList<>();
}
