package com.example.shopping.supplier.document.repository;

import com.example.shopping.supplier.document.entity.SupplierDocument;
import com.example.shopping.supplier.document.enums.SupplierDocumentStatus;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SupplierDocumentRepository extends JpaRepository<SupplierDocument, UUID> {
    @EntityGraph(attributePaths = "supplier")
    List<SupplierDocument> findAllByOrderBySubmittedAtDesc();
    @EntityGraph(attributePaths = "supplier")
    List<SupplierDocument> findByStatusOrSubmittedByOrderBySubmittedAtDesc(SupplierDocumentStatus status, String username);
    boolean existsBySupplier_Id(UUID supplierId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from SupplierDocument d where d.id = :id")
    Optional<SupplierDocument> findForReview(@Param("id") UUID id);
}
