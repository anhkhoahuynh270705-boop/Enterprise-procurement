package com.example.shopping.supplier.proposal.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import com.example.shopping.supplier.proposal.enums.SupplierProposalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "supplier_proposals", indexes = {
    @Index(name = "idx_supplier_proposal_owner", columnList = "submittedBy"),
    @Index(name = "idx_supplier_proposal_status", columnList = "status")
})
@Getter
@Setter
public class SupplierProposal {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 13) private String taxCode;
    @Column(length = 200) private String contactPerson;
    @Column(length = 254) private String email;
    @Column(length = 16) private String phone;
    private String address;
    private String city;
    private String country;
    private String bankName;
    private String bankAccount;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false, length = 2000) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private SupplierProposalStatus status = SupplierProposalStatus.PENDING;
    @Column(nullable = false) private String submittedBy;
    @Column(nullable = false) private LocalDateTime submittedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    @Column(length = 2000) private String reviewComment;
    private UUID supplierId;

    // PostgreSQL permits multiple NULL values, freeing the reservation after review.
    @Column(unique = true, length = 13)
    private String pendingTaxCode;
}
