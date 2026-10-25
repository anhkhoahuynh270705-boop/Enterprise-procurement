package com.example.shopping.requestForQuotation.entity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import com.example.shopping.procurement.entity.ProcurementItem;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rfq_items", indexes = {
        @Index(name = "idx_rfq_items_rfq", columnList = "rfq_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfqItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_item_id")
    private ProcurementItem procurementItem;

    @Column(length = 50)
    private String sku;

    @Column(name = "item_name", nullable = false, length = 255)
    private String itemName;

    @Column(length = 100)
    private String category;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 50)
    private String unit;

    @Column(name = "budget_unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetUnitPrice;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "rfqItem", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<RfqQuoteItem> quoteItems = new ArrayList<>();

    @OneToMany(mappedBy = "rfqItem", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<RfqItemAward> awards = new ArrayList<>();
}
