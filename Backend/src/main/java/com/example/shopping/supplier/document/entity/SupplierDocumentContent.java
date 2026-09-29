package com.example.shopping.supplier.document.entity;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "supplier_document_contents") @Getter @Setter
public class SupplierDocumentContent {
    @Id private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @MapsId
    @JoinColumn(name = "id") private SupplierDocument document;
    @Column(nullable = false, columnDefinition = "bytea") private byte[] bytes;
}
