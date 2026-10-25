package com.example.shopping.requestForQuotation.report.exporter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.shopping.common.report.service.JasperReportService;
import com.example.shopping.requestForQuotation.dto.reportdto.RfqComparisonPdfRow;
import com.example.shopping.requestForQuotation.dto.response.ItemResponse;
import com.example.shopping.requestForQuotation.dto.response.QuoteItemResponse;
import com.example.shopping.requestForQuotation.dto.response.QuoteScoreResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqDetailResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;
import com.example.shopping.requestForQuotation.dto.response.ScoreCriterionResponse;
import com.example.shopping.requestForQuotation.enums.RfqScoringCriterion;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class RfqPdfExporter {

    private static final String TEMPLATE = "/reports/rfq-comparison.jrxml";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    JasperReportService jasperReportService;

    public byte[] exportComparison(RfqDetailResponse rfq) throws Exception {
        List<RfqQuoteRoundResponse> quotes = latestSubmittedRounds(rfq.quoteRounds());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("RFQ_CODE", text(rfq.rfqCode()));
        parameters.put("RFQ_TITLE", text(rfq.title()));
        parameters.put("PR_CODE", text(rfq.procurementTicketCode()));
        parameters.put("DEPARTMENT", text(rfq.department()));
        parameters.put("CREATOR", text(rfq.creatorName() == null ? rfq.createdBy() : rfq.creatorName()));
        parameters.put("STATUS", statusLabel(rfq.status() == null ? "" : rfq.status().name()));
        parameters.put("DEADLINE", dateTime(rfq.deadline()));
        parameters.put("CURRENCY", text(rfq.currency()));
        parameters.put("EXPORTED_AT", DATE_TIME.format(LocalDateTime.now()));
        parameters.put("QUOTE_COUNT", Integer.toString(quotes.size()));
        parameters.put("SCORING_WEIGHTS", scoringWeights(rfq.scoreCriteria()));
        parameters.put("SELECTED_SUPPLIER", text(rfq.selectedSupplierName()));
        parameters.put("SELECTION_REASON", text(rfq.selectionReason()));

        return jasperReportService.generatePdf(TEMPLATE, parameters, buildRows(rfq, quotes));
    }

    private List<RfqComparisonPdfRow> buildRows(
            RfqDetailResponse rfq,
            List<RfqQuoteRoundResponse> quotes) {
        List<ItemResponse> items = rfq.items() == null ? List.of() : rfq.items();
        if (quotes.isEmpty()) {
            return List.of(RfqComparisonPdfRow.builder()
                    .groupKey("NO_QUOTES")
                    .supplierName("Chưa nhận được báo giá")
                    .roundNumber("—")
                    .itemName("Chưa có báo giá để so sánh")
                    .quantity("—")
                    .unit("—")
                    .budgetUnitPrice("—")
                    .unitPrice("—")
                    .lineTotal("—")
                    .budgetVariance("—")
                    .subtotalBeforeTax("—")
                    .taxAmount("—")
                    .shippingCost("—")
                    .otherCost("—")
                    .totalAfterTax("—")
                    .deliveryDays("—")
                    .paymentTerms("—")
                    .warranty("—")
                    .validUntil("—")
                    .technicalStatus("—")
                    .totalScore("—")
                    .rank("—")
                    .priceScore("—")
                    .technicalScore("—")
                    .deliveryScore("—")
                    .paymentScore("—")
                    .reputationScore("—")
                    .build());
        }

        List<RfqComparisonPdfRow> rows = new ArrayList<>();
        for (RfqQuoteRoundResponse quote : quotes) {
            Map<UUID, QuoteItemResponse> quoteItems = new LinkedHashMap<>();
            if (quote.items() != null) {
                quote.items().forEach(item -> quoteItems.put(item.rfqItemId(), item));
            }
            Map<RfqScoringCriterion, BigDecimal> scores = new EnumMap<>(RfqScoringCriterion.class);
            if (quote.scores() != null) {
                for (QuoteScoreResponse score : quote.scores()) {
                    if (score.criterion() != null) scores.put(score.criterion(), score.score());
                }
            }

            for (ItemResponse item : items) {
                QuoteItemResponse line = quoteItems.get(item.id());
                boolean quoted = line != null && line.quoted() && line.unitPrice() != null;
                BigDecimal unitPrice = quoted ? line.unitPrice() : null;
                BigDecimal lineTotal = quoted
                        ? unitPrice.multiply(BigDecimal.valueOf(item.quantity()))
                        : null;

                rows.add(RfqComparisonPdfRow.builder()
                        .groupKey(quote.supplierId().toString())
                        .supplierName(text(quote.supplierName()))
                        .roundNumber(Integer.toString(quote.roundNumber()))
                        .sku(text(item.sku()))
                        .itemName(text(item.itemName()))
                        .quantity(Integer.toString(item.quantity()))
                        .unit(text(item.unit()))
                        .budgetUnitPrice(money(item.budgetUnitPrice(), rfq.currency()))
                        .unitPrice(quoted ? money(unitPrice, rfq.currency()) : "Không chào")
                        .lineTotal(quoted ? money(lineTotal, rfq.currency()) : "—")
                        .budgetVariance(variance(unitPrice, item.budgetUnitPrice()))
                        .subtotalBeforeTax(money(quote.subtotalBeforeTax(), rfq.currency()))
                        .taxAmount(money(quote.taxAmount(), rfq.currency()))
                        .shippingCost(money(quote.shippingCost(), rfq.currency()))
                        .otherCost(money(quote.otherCost(), rfq.currency()))
                        .totalAfterTax(money(quote.totalAfterTax(), rfq.currency()))
                        .deliveryDays(quote.deliveryDays() == null ? "—" : quote.deliveryDays() + " ngày")
                        .paymentTerms(text(quote.paymentTerms()))
                        .warranty(text(quote.warranty()))
                        .validUntil(date(quote.validUntil()))
                        .technicalStatus(technicalLabel(quote.technicalStatus() == null
                                ? "" : quote.technicalStatus().name()))
                        .totalScore(score(quote.totalScore()))
                        .rank(quote.rank() == null ? "—" : "Hạng " + quote.rank())
                        .priceScore(score(scores.get(RfqScoringCriterion.PRICE)))
                        .technicalScore(score(scores.get(RfqScoringCriterion.TECHNICAL)))
                        .deliveryScore(score(scores.get(RfqScoringCriterion.DELIVERY)))
                        .paymentScore(score(scores.get(RfqScoringCriterion.PAYMENT)))
                        .reputationScore(score(scores.get(RfqScoringCriterion.REPUTATION)))
                        .build());
            }
        }
        return rows;
    }

    private List<RfqQuoteRoundResponse> latestSubmittedRounds(List<RfqQuoteRoundResponse> rounds) {
        if (rounds == null || rounds.isEmpty()) return List.of();
        Map<UUID, RfqQuoteRoundResponse> latestBySupplier = new LinkedHashMap<>();
        rounds.stream()
                .filter(round -> round.status() != null && "SUBMITTED".equals(round.status().name()))
                .sorted(Comparator.comparingInt(RfqQuoteRoundResponse::roundNumber))
                .forEach(round -> latestBySupplier.put(round.supplierId(), round));
        return latestBySupplier.values().stream()
                .sorted(Comparator.comparing(RfqQuoteRoundResponse::supplierName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private String scoringWeights(List<ScoreCriterionResponse> criteria) {
        if (criteria == null || criteria.isEmpty()) return "Chưa cấu hình trọng số";
        return criteria.stream()
                .map(item -> criterionLabel(item.criterion()) + ": " + score(item.weightPercent()) + "%")
                .reduce((left, right) -> left + "  ·  " + right)
                .orElse("Chưa cấu hình trọng số");
    }

    private String criterionLabel(RfqScoringCriterion criterion) {
        if (criterion == null) return "Tiêu chí";
        return switch (criterion) {
            case PRICE -> "Giá";
            case TECHNICAL -> "Kỹ thuật / chất lượng";
            case DELIVERY -> "Giao hàng";
            case PAYMENT -> "Thanh toán";
            case REPUTATION -> "Uy tín NCC";
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "DRAFT" -> "Nháp";
            case "PENDING_RFQ_APPROVAL" -> "Chờ duyệt RFQ";
            case "RETURNED" -> "Bị trả lại";
            case "SENT" -> "Đã gửi";
            case "RECEIVING_QUOTES" -> "Đang nhận báo giá";
            case "CLOSED_QUOTES" -> "Đã đóng nhận giá";
            case "COMPARING" -> "Đang so sánh";
            case "PENDING_RESULT_APPROVAL" -> "Chờ duyệt kết quả";
            case "SELECTED_SUPPLIER" -> "Đã chọn NCC";
            case "PO_CREATED" -> "Đã tạo PO";
            case "REJECTED" -> "Từ chối";
            case "CANCELED" -> "Đã hủy";
            case "EXPIRED" -> "Hết hạn";
            default -> "—";
        };
    }

    private String technicalLabel(String status) {
        return switch (status) {
            case "PASS" -> "Đạt";
            case "PARTIAL" -> "Đạt một phần";
            case "FAIL" -> "Không đạt";
            default -> "Chưa đánh giá";
        };
    }

    private String variance(BigDecimal unitPrice, BigDecimal budget) {
        if (unitPrice == null || budget == null || budget.signum() <= 0) return "—";
        BigDecimal percentage = unitPrice.subtract(budget)
                .multiply(BigDecimal.valueOf(100))
                .divide(budget, 1, RoundingMode.HALF_UP);
        return (percentage.signum() > 0 ? "+" : "") + percentage.stripTrailingZeros().toPlainString() + "%";
    }

    private String money(BigDecimal value, String currency) {
        if (value == null) return "—";
        NumberFormat number = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
        return number.format(value.setScale(0, RoundingMode.HALF_UP)) + " " + text(currency);
    }

    private String score(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

    private String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    private String dateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "—" : value.trim();
    }

}
