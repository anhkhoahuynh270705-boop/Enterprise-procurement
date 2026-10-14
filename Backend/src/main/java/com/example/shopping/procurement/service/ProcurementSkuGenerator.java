package com.example.shopping.procurement.service;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.shopping.procurement.repository.ProcurementItemRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProcurementSkuGenerator {

    JdbcTemplate jdbcTemplate;
    ProcurementItemRepository itemRepository;

    @PostConstruct
    void initializeSequence() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS procurement_item_sku_seq START WITH 1 INCREMENT BY 1");
    }

    public String generateNextSku() {
        while (true) {
            Long sequence = jdbcTemplate.queryForObject(
                    "SELECT nextval('procurement_item_sku_seq')", Long.class);
            String sku = String.format("SKU-%03d", sequence);
            if (!itemRepository.existsByItemCodeIgnoreCase(sku)) {
                return sku;
            }
        }
    }
}
