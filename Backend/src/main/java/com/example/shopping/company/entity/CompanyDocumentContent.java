package com.example.shopping.company.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity 
@Table(name = "company_document_contents") 
@Getter 
@Setter
public class CompanyDocumentContent {
    @Id 
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false) 
    @MapsId
    @JoinColumn(name = "id") 
    private CompanyDocument document;

    @Column(nullable = false, columnDefinition = "bytea") 
    private byte[] bytes;
}
