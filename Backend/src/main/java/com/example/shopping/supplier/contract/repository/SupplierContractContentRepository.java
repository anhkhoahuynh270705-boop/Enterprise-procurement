package com.example.shopping.supplier.contract.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopping.supplier.contract.entity.SupplierContractContent;

public interface SupplierContractContentRepository extends JpaRepository<SupplierContractContent, UUID> {
}
