package com.example.shopping.requestForQuotation.repository;

import com.example.shopping.requestForQuotation.entity.Rfq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface RfqRepository extends JpaRepository<Rfq, UUID>, JpaSpecificationExecutor<Rfq> {
}
