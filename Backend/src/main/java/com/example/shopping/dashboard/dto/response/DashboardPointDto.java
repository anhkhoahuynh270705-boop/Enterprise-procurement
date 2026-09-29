package com.example.shopping.dashboard.dto.response;

import java.math.BigDecimal;

public record DashboardPointDto(
    String key, 
    String label, 
    BigDecimal value
) { }
