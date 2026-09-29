package com.example.shopping.supplier.proposal.repository;

import com.example.shopping.supplier.proposal.entity.SupplierProposal;
import com.example.shopping.supplier.proposal.enums.SupplierProposalStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SupplierProposalRepository extends JpaRepository<SupplierProposal, UUID> {
    boolean existsByPendingTaxCode(String taxCode);
    List<SupplierProposal> findAllByOrderBySubmittedAtDesc();
    List<SupplierProposal> findBySubmittedByOrderBySubmittedAtDesc(String username);
    long countByStatus(SupplierProposalStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SupplierProposal p where p.id = :id")
    Optional<SupplierProposal> findForReview(@Param("id") UUID id);
}
