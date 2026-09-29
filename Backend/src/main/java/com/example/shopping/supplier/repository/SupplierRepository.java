package com.example.shopping.supplier.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.shopping.supplier.entity.SupplierEntity;
import com.example.shopping.supplier.enums.SupplierStatus;

@Repository
public interface SupplierRepository extends JpaRepository<SupplierEntity, UUID> {

    Optional<SupplierEntity> findByCode(String code);

    Optional<SupplierEntity> findByTaxCode(String taxCode);

    boolean existsByCode(String code);

    boolean existsByTaxCode(String taxCode);

    List<SupplierEntity> findByStatus(SupplierStatus status);

    @Query("SELECT s FROM SupplierEntity s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SupplierEntity> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT MAX(s.code) FROM SupplierEntity s WHERE s.code LIKE :prefix%")
    Optional<String> findMaxCodeByPrefix(@Param("prefix") String prefix);
}
