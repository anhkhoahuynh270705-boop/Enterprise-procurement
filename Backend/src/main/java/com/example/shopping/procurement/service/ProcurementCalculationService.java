package com.example.shopping.procurement.service;

import com.example.shopping.procurement.entity.ProcurementTicket;

public interface ProcurementCalculationService {
    void recalculate(ProcurementTicket ticket);
}
