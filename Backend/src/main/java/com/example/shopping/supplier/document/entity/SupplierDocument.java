package com.example.shopping.supplier.document.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.example.shopping.supplier.entity.SupplierEntity;
import com.example.shopping.supplier.document.enums.SupplierDocumentStatus;

@Entity @Table(name = "supplier_documents", indexes = {
    @Index(name = "idx_supplier_document_supplier", columnList = "supplier_id"),
    @Index(name = "idx_supplier_document_owner", columnList = "submitted_by")
}) @Getter @Setter
public class SupplierDocument {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false) private SupplierEntity supplier;
    @Column(nullable = false, length = 200) private String filename;
    @Column(nullable = false) private String contentType;
    @Column(nullable = false) private long size;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SupplierDocumentStatus status;
    @Column(name = "submitted_by", nullable = false) private String submittedBy;
    @Column(nullable = false) private LocalDateTime submittedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    @Column(length = 2000) private String reviewComment;
}
