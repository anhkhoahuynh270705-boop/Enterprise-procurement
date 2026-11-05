package com.example.shopping.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.shopping.company.entity.CompanyDocument;

import java.util.*;

public interface CompanyDocumentRepository extends
        JpaRepository<CompanyDocument, UUID>,
        JpaSpecificationExecutor<CompanyDocument> {
    List<CompanyDocument> findByDeletedFalseOrderByUploadedAtDesc();
    Optional<CompanyDocument> findByIdAndDeletedFalse(UUID id);
}
