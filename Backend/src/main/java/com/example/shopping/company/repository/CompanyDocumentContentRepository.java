package com.example.shopping.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopping.company.entity.CompanyDocumentContent;

import java.util.UUID;

public interface CompanyDocumentContentRepository extends JpaRepository<CompanyDocumentContent, UUID> {}
