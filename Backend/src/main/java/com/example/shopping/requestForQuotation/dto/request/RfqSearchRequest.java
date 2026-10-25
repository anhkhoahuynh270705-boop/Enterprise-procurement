package com.example.shopping.requestForQuotation.dto.request;

import java.time.LocalDate;

import com.example.shopping.requestForQuotation.enums.RfqStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record RfqSearchRequest(
        String keyword,
        RfqStatus status,
        String department,
        String createdBy,
        LocalDate createdFrom,
        LocalDate createdTo,
        @Min(0)
        int page,
        @Min(1)
        @Max(100)
        int size
) {}
