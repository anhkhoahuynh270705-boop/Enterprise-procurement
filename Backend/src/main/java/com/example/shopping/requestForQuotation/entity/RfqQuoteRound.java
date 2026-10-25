package com.example.shopping.requestForQuotation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.shopping.requestForQuotation.enums.RfqQuoteStatus;
import com.example.shopping.requestForQuotation.enums.RfqTechnicalStatus;
import com.example.shopping.supplier.entity.SupplierEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "rfq_quote_rounds",
        indexes = @Index(name = "idx_rfq_quote_rounds_rfq_supplier", columnList = "rfq_id, supplier_id, round_number"),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_rfq_supplier_round",
                columnNames = {"rfq_id", "supplier_id", "round_number"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfqQuoteRound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierEntity supplier;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RfqQuoteStatus status = RfqQuoteStatus.SUBMITTED;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    @Column(name = "payment_terms", length = 1000)
    private String paymentTerms;

    @Column(length = 1000)
    private String warranty;

    @Enumerated(EnumType.STRING)
    @Column(name = "technical_status", length = 20)
    private RfqTechnicalStatus technicalStatus;

    @Column(name = "subtotal_before_tax", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal subtotalBeforeTax = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_cost", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "other_cost", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal otherCost = BigDecimal.ZERO;

    @Column(name = "total_after_tax", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal totalAfterTax = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "quoteRound", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<RfqQuoteItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "quoteRound", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<RfqQuoteScore> scores = new ArrayList<>();
}
