package com.example.shopping.requestForQuotation.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import com.example.shopping.requestForQuotation.enums.RfqInviteStatus;
import com.example.shopping.supplier.entity.SupplierEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "rfq_invited_suppliers",
        indexes = @Index(name = "idx_rfq_invited_supplier", columnList = "supplier_id, status"),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_rfq_invited_supplier",
                columnNames = {"rfq_id", "supplier_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfqInvitedSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private SupplierEntity supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RfqInviteStatus status = RfqInviteStatus.INVITED;

    @Column(name = "invited_at", nullable = false)
    private LocalDateTime invitedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
