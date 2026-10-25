package com.example.shopping.requestForQuotation.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "rfq_quote_items",
        indexes = @Index(name = "idx_rfq_quote_items_round", columnList = "quote_round_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_quote_round_item",
                columnNames = {"quote_round_id", "rfq_item_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfqQuoteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_round_id", nullable = false)
    private RfqQuoteRound quoteRound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_item_id", nullable = false)
    private RfqItem rfqItem;

    @Column(name = "is_quoted", nullable = false)
    @Builder.Default
    private Boolean quoted = true;

    @Column(name = "unit_price", precision = 18, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "note", length = 1000)
    private String note;
}
