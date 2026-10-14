package com.example.shopping.supplier.contract.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "supplier_contract_number_counters")
@Getter
@Setter
@NoArgsConstructor
public class SupplierContractNumberCounter {

    @Id
    @Column(name = "contract_year")
    private Integer contractYear;

    @Column(name = "last_number", nullable = false)
    private Long lastNumber;
}
