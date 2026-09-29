package com.example.shopping.company.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_documents")
@Getter @Setter
public class CompanyDocument {
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID) 
    private UUID id;

    @Column(nullable = false, length = 200) 
    private String title;

    @Column(nullable = false, length = 30) 
    private String category;

    @Column(nullable = false, length = 2000) 
    private String description;

    @Column(nullable = false, length = 200) 
    private String filename;

    @Column(nullable = false) 
    private String contentType;

    @Column(nullable = false) 
    private long size;

    @Column(nullable = false) 
    private String uploadedBy;

    @Column(nullable = false) 
    private LocalDateTime uploadedAt;
    
    @Column(nullable = false) 
    private boolean deleted;
}
