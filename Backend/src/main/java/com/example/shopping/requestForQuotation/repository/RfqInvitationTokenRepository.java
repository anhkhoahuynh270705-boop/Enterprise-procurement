package com.example.shopping.requestForQuotation.repository;

import com.example.shopping.requestForQuotation.entity.RfqInvitationToken;
import jakarta.persistence.LockModeType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RfqInvitationTokenRepository extends JpaRepository<RfqInvitationToken, UUID> {
    @Query("""
        SELECT t
        FROM RfqInvitationToken t
        JOIN FETCH t.invitedSupplier i
        JOIN FETCH i.rfq r
        JOIN FETCH i.supplier s
        WHERE t.tokenHash = :hash
        """)
    Optional<RfqInvitationToken> findByTokenHash(@Param("hash") String hash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t
        FROM RfqInvitationToken t
        JOIN FETCH t.invitedSupplier i
        JOIN FETCH i.rfq r
        JOIN FETCH i.supplier s
        WHERE t.tokenHash = :hash
        """)
    Optional<RfqInvitationToken> findByHashForUpdate(@Param("hash") String hash);

    @Modifying(flushAutomatically = true)
    @Query("""
        update RfqInvitationToken t
        set t.revokedAt = CURRENT_TIMESTAMP
        where t.invitedSupplier.id = :inviteId
          and t.consumedAt is null
          and t.revokedAt is null
        """)
    @Transactional
    int revokeActiveTokens(@Param("inviteId") UUID inviteId);
}
