package com.example.shopping.supplier.contract.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "supplier_contract_contents")
@Getter
@Setter
@NoArgsConstructor
public class SupplierContractContent {

    @Id
    private UUID id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "id")
    private SupplierContract contract;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] bytes;
}
