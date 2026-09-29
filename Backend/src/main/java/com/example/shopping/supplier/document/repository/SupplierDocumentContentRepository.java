package com.example.shopping.supplier.document.repository;

import com.example.shopping.supplier.document.entity.SupplierDocumentContent;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierDocumentContentRepository extends JpaRepository<SupplierDocumentContent, UUID> { }
