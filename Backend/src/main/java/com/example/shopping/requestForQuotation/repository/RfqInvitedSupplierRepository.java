package com.example.shopping.requestForQuotation.repository;

import com.example.shopping.requestForQuotation.entity.RfqInvitedSupplier;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RfqInvitedSupplierRepository extends JpaRepository<RfqInvitedSupplier, UUID> {
    Optional<RfqInvitedSupplier> findByRfq_IdAndSupplier_Id(UUID rfqId, UUID supplierId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM RfqInvitedSupplier i WHERE i.id = :id")
    Optional<RfqInvitedSupplier> findByIdForUpdate(@Param("id") UUID id);
}
