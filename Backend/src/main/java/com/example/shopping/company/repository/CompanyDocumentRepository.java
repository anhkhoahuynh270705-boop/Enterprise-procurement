package com.example.shopping.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopping.company.entity.CompanyDocument;

import java.util.*;

public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, UUID> {
    List<CompanyDocument> findByDeletedFalseOrderByUploadedAtDesc();
    Optional<CompanyDocument> findByIdAndDeletedFalse(UUID id);
}
