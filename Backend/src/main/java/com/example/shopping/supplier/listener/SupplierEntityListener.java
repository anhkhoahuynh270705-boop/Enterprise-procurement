package com.example.shopping.supplier.listener;

import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import com.example.shopping.supplier.entity.SupplierEntity;

public class SupplierEntityListener {
    @PrePersist
    public void onCreate(SupplierEntity supplier) {
        supplier.setCreatedAt(LocalDateTime.now());
        supplier.setUpdatedAt(LocalDateTime.now());
    }

    @PreUpdate
    public void onUpdate(SupplierEntity supplier) {
        supplier.setUpdatedAt(LocalDateTime.now());
    }
}
