package com.example.shopping.supplier.contract.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierContractNumberCounterRepository {

    private static final String NEXT_NUMBER_SQL = """
            INSERT INTO supplier_contract_number_counters (contract_year, last_number)
            VALUES (?, 1)
            ON CONFLICT (contract_year)
            DO UPDATE SET last_number = supplier_contract_number_counters.last_number + 1
            RETURNING last_number
            """;

    JdbcTemplate jdbcTemplate;

    public Long nextNumberForYear(int year) {

        return jdbcTemplate.queryForObject(NEXT_NUMBER_SQL, Long.class, year);
    }
}
