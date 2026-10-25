package com.example.shopping.requestForQuotation.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.shopping.supplier.entity.SupplierEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "rfq_item_awards",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_rfq_item_award",
                columnNames = {"rfq_id", "rfq_item_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfqItemAward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_item_id", nullable = false)
    private RfqItem rfqItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierEntity supplier;

    @Column(length = 2000)
    private String reason;

    @Column(name = "selected_by", nullable = false, length = 100)
    private String selectedBy;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;
}
