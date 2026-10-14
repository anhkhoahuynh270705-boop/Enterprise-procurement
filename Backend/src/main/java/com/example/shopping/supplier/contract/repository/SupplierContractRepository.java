package com.example.shopping.supplier.contract.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopping.supplier.contract.entity.SupplierContract;

public interface SupplierContractRepository extends JpaRepository<SupplierContract, UUID> {

    @EntityGraph(attributePaths = "supplier")
    List<SupplierContract> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "supplier")
    Optional<SupplierContract> findWithSupplierById(UUID id);

    boolean existsByContractNumberIgnoreCase(String contractNumber);
}
