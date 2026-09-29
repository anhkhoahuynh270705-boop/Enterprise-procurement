package com.example.shopping.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shopping.user.entity.UserEntity;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    boolean existsByEmployeeId(String employeeId);

    boolean existsByEmployeeIdAndIdNot(String employeeId, UUID id);

    long countByEnabledTrue();

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findById(UUID id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
