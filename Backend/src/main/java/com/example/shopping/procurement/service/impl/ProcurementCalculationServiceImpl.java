package com.example.shopping.procurement.service.impl;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.service.ProcurementCalculationService;

@Service
public class ProcurementCalculationServiceImpl implements ProcurementCalculationService {
    @Override
    public void recalculate(ProcurementTicket ticket) {
        BigDecimal total = BigDecimal.ZERO;
        if (ticket.getItems() != null) {
            for (var item : ticket.getItems()) {
                BigDecimal price = (item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice())
                    .multiply(BigDecimal.valueOf(item.getQuantity() == null ? 1 : item.getQuantity()));
                item.setTotalPrice(price);
                total = total.add(price);
            }
        }
        ticket.setTotalAmount(total);
    }
}
