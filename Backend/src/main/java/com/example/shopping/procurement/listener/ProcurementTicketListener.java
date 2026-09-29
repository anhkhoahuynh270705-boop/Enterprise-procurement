package com.example.shopping.procurement.listener;

import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import com.example.shopping.procurement.entity.ProcurementTicket;

public class ProcurementTicketListener {
    @PrePersist
    public void onCreate(ProcurementTicket ticket) {
        if (ticket.getCreatedAt() == null) ticket.setCreatedAt(LocalDateTime.now());
        if (ticket.getUpdatedAt() == null) ticket.setUpdatedAt(LocalDateTime.now());
    }

    @PreUpdate
    public void onUpdate(ProcurementTicket ticket) { ticket.setUpdatedAt(LocalDateTime.now()); }
}
