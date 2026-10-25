package com.example.shopping.requestForQuotation.dto.reportdto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RfqComparisonPdfRow {
    private String groupKey;
    private String supplierName;
    private String roundNumber;
    private String sku;
    private String itemName;
    private String quantity;
    private String unit;
    private String budgetUnitPrice;
    private String unitPrice;
    private String lineTotal;
    private String budgetVariance;
    private String subtotalBeforeTax;
    private String taxAmount;
    private String shippingCost;
    private String otherCost;
    private String totalAfterTax;
    private String deliveryDays;
    private String paymentTerms;
    private String warranty;
    private String validUntil;
    private String technicalStatus;
    private String totalScore;
    private String rank;
    private String priceScore;
    private String technicalScore;
    private String deliveryScore;
    private String paymentScore;
    private String reputationScore;
}
