package com.example.shopping.requestForQuotation.report.exporter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.example.shopping.requestForQuotation.dto.response.ItemResponse;
import com.example.shopping.requestForQuotation.dto.response.QuoteItemResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqDetailResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqSummaryResponse;

@Component
public class RfqExcelExporter {
    public static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] exportList(List<RfqSummaryResponse> rfqs, String filterSummary) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            SheetStyles styles = new SheetStyles(workbook);
            var sheet = workbook.createSheet("Danh sách RFQ");
            sheet.setDisplayGridlines(false);
            sheet.setDefaultRowHeight((short) 360);

            String[] headers = {"MÃ RFQ", "TIÊU ĐỀ", "MÃ PR", "PHÒNG BAN", "BÁO GIÁ", "HẠN NỘP", "GIÁ TRỊ ƯỚC TÍNH", "TRẠNG THÁI", "NGƯỜI TẠO", "NGÀY TẠO"};
            title(sheet, 0, headers.length - 1, "DANH SÁCH YÊU CẦU BÁO GIÁ", styles.title);
            title(sheet, 1, headers.length - 1,
                    "ENTERPRISE PROCUREMENT  ·  Xuất lúc " + DATE_TIME.format(LocalDateTime.now()), styles.subtitle);
            title(sheet, 2, headers.length - 1,
                    "Bộ lọc: " + (filterSummary == null || filterSummary.isBlank() ? "Tất cả RFQ" : filterSummary),
                    styles.filter);
            title(sheet, 3, headers.length - 1, "Tổng số: " + rfqs.size() + " RFQ", styles.summary);

            int headerRowIndex = 5;
            Row header = sheet.createRow(headerRowIndex);
            for (int i = 0; i < headers.length; i++) cell(header, i, headers[i], styles.header);

            int rowIndex = headerRowIndex + 1;
            for (int index = 0; index < rfqs.size(); index++) {
                RfqSummaryResponse rfq = rfqs.get(index);
                Row row = sheet.createRow(rowIndex++);
                CellStyle base = index % 2 == 0 ? styles.body : styles.bodyAlt;
                cell(row, 0, text(rfq.rfqCode()), styles.code);
                cell(row, 1, text(rfq.title()), base);
                cell(row, 2, text(rfq.procurementTicketCode()), base);
                cell(row, 3, text(rfq.department()), base);
                cell(row, 4, rfq.respondedSupplierCount() + "/" + rfq.invitedSupplierCount(), styles.center);
                cell(row, 5, dateTime(rfq.deadline()), styles.date);
                numeric(row, 6, rfq.estimatedValue(), styles.money);
                cell(row, 7, statusLabel(rfq.status() == null ? "" : rfq.status().name()), statusStyle(styles, rfq.status() == null ? "" : rfq.status().name()));
                cell(row, 8, text(rfq.creatorName() == null ? rfq.createdBy() : rfq.creatorName()), base);
                cell(row, 9, dateTime(rfq.createdAt()), styles.date);
            }

            int lastRow = Math.max(headerRowIndex, rowIndex - 1);
            sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, lastRow, 0, headers.length - 1));
            sheet.createFreezePane(0, headerRowIndex + 1);
            int[] widths = {22, 42, 20, 24, 14, 23, 24, 25, 24, 23};
            for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
            sheet.setZoom(90);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo file Excel danh sách RFQ.", exception);
        }
    }

    public byte[] exportComparison(RfqDetailResponse rfq) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            SheetStyles styles = new SheetStyles(workbook);
            var sheet = workbook.createSheet("So sánh báo giá");
            sheet.setDisplayGridlines(false);
            sheet.setDefaultRowHeight((short) 360);

            List<RfqQuoteRoundResponse> quotes = latestRounds(rfq.quoteRounds());
            int lastColumn = Math.max(4, 4 + quotes.size());
            title(sheet, 0, lastColumn, "BIÊN BẢN SO SÁNH BÁO GIÁ", styles.title);
            title(sheet, 1, lastColumn,
                    text(rfq.rfqCode()) + "  ·  " + text(rfq.title()), styles.subtitle);
            title(sheet, 2, lastColumn,
                    "PR: " + text(rfq.procurementTicketCode()) + "    |    Phòng ban: " + text(rfq.department())
                            + "    |    Trạng thái: " + statusLabel(rfq.status() == null ? "" : rfq.status().name()),
                    styles.filter);
            title(sheet, 3, lastColumn,
                    "Hạn báo giá: " + dateTime(rfq.deadline()) + "    |    Đơn vị tiền: " + text(rfq.currency())
                            + "    |    Xuất lúc: " + DATE_TIME.format(LocalDateTime.now()), styles.summary);

            String[] fixedHeaders = {"MÃ HÀNG", "HẠNG MỤC", "SỐ LƯỢNG", "ĐVT", "NGÂN SÁCH / ĐƠN VỊ"};
            int headerRowIndex = 5;
            Row header = sheet.createRow(headerRowIndex);
            for (int i = 0; i < fixedHeaders.length; i++) cell(header, i, fixedHeaders[i], styles.header);
            for (int i = 0; i < quotes.size(); i++) {
                cell(header, fixedHeaders.length + i,
                        text(quotes.get(i).supplierName()) + " · V" + quotes.get(i).roundNumber(), styles.header);
            }

            Map<UUID, Map<UUID, QuoteItemResponse>> quoteItems = new LinkedHashMap<>();
            for (RfqQuoteRoundResponse quote : quotes) {
                Map<UUID, QuoteItemResponse> items = new LinkedHashMap<>();
                if (quote.items() != null) {
                    for (QuoteItemResponse item : quote.items()) items.put(item.rfqItemId(), item);
                }
                quoteItems.put(quote.id(), items);
            }

            List<ItemResponse> items = rfq.items() == null ? List.of() : rfq.items();
            int rowIndex = headerRowIndex + 1;
            for (int index = 0; index < items.size(); index++) {
                ItemResponse item = items.get(index);
                Row row = sheet.createRow(rowIndex++);
                CellStyle body = index % 2 == 0 ? styles.body : styles.bodyAlt;
                cell(row, 0, text(item.sku()), styles.code);
                cell(row, 1, text(item.itemName()), body);
                numeric(row, 2, BigDecimal.valueOf(item.quantity()), styles.center);
                cell(row, 3, text(item.unit()), styles.center);
                numeric(row, 4, item.budgetUnitPrice(), styles.money);

                List<BigDecimal> prices = new ArrayList<>();
                for (RfqQuoteRoundResponse quote : quotes) {
                    QuoteItemResponse line = quoteItems.get(quote.id()).get(item.id());
                    if (line != null && line.quoted() && line.unitPrice() != null) prices.add(line.unitPrice());
                }
                BigDecimal lowest = prices.stream().min(Comparator.naturalOrder()).orElse(null);
                BigDecimal highest = prices.stream().max(Comparator.naturalOrder()).orElse(null);
                for (int i = 0; i < quotes.size(); i++) {
                    RfqQuoteRoundResponse quote = quotes.get(i);
                    QuoteItemResponse line = quoteItems.get(quote.id()).get(item.id());
                    int column = fixedHeaders.length + i;
                    if (line == null || !line.quoted() || line.unitPrice() == null) {
                        cell(row, column, "Không chào", styles.missing);
                    } else {
                        CellStyle comparisonStyle = styles.body;
                        if (item.budgetUnitPrice() != null && line.unitPrice().compareTo(item.budgetUnitPrice()) > 0) comparisonStyle = styles.overBudget;
                        else if (lowest != null && line.unitPrice().compareTo(lowest) == 0) comparisonStyle = styles.lowest;
                        else if (highest != null && line.unitPrice().compareTo(highest) == 0) comparisonStyle = styles.highest;
                        numeric(row, column, line.unitPrice(), comparisonStyle);
                    }
                }
            }

            int lastItemRow = Math.max(headerRowIndex, rowIndex - 1);
            addSummaryRow(sheet, rowIndex++, "TỔNG GIÁ TRỊ TRƯỚC VAT", quotes, styles, QuoteMetric.SUBTOTAL);
            addSummaryRow(sheet, rowIndex++, "THUẾ VAT", quotes, styles, QuoteMetric.TAX);
            addSummaryRow(sheet, rowIndex++, "PHÍ VẬN CHUYỂN", quotes, styles, QuoteMetric.SHIPPING);
            addSummaryRow(sheet, rowIndex++, "CHI PHÍ KHÁC", quotes, styles, QuoteMetric.OTHER);
            addSummaryRow(sheet, rowIndex++, "TỔNG CHI PHÍ SAU VAT (TCO)", quotes, styles, QuoteMetric.TOTAL);
            addSummaryRow(sheet, rowIndex++, "THỜI GIAN GIAO HÀNG (NGÀY)", quotes, styles, QuoteMetric.DELIVERY);
            addSummaryRow(sheet, rowIndex++, "ĐIỀU KHOẢN THANH TOÁN", quotes, styles, QuoteMetric.PAYMENT);
            addSummaryRow(sheet, rowIndex++, "BẢO HÀNH", quotes, styles, QuoteMetric.WARRANTY);
            addSummaryRow(sheet, rowIndex++, "HIỆU LỰC BÁO GIÁ", quotes, styles, QuoteMetric.VALIDITY);
            addSummaryRow(sheet, rowIndex++, "TUÂN THỦ KỸ THUẬT", quotes, styles, QuoteMetric.TECHNICAL);
            addSummaryRow(sheet, rowIndex++, "TỔNG ĐIỂM", quotes, styles, QuoteMetric.SCORE);
            addSummaryRow(sheet, rowIndex, "XẾP HẠNG", quotes, styles, QuoteMetric.RANK);

            sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, lastItemRow,
                    0, fixedHeaders.length + quotes.size() - 1));
            sheet.createFreezePane(fixedHeaders.length, headerRowIndex + 1);
            int[] fixedWidths = {18, 38, 14, 12, 23};
            for (int i = 0; i < fixedWidths.length; i++) sheet.setColumnWidth(i, fixedWidths[i] * 256);
            for (int i = 0; i < quotes.size(); i++) sheet.setColumnWidth(fixedHeaders.length + i, 28 * 256);
            sheet.setZoom(85);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo biên bản Excel so sánh báo giá.", exception);
        }
    }

    private void addSummaryRow(Sheet sheet, int rowIndex, String label, List<RfqQuoteRoundResponse> quotes, SheetStyles styles, QuoteMetric metric) {
        Row row = sheet.createRow(rowIndex);
        cell(row, 0, label, styles.summaryLabel);
        for (int i = 1; i < 5; i++) cell(row, i, "", styles.summaryLabel);
        for (int i = 0; i < quotes.size(); i++) {
            RfqQuoteRoundResponse quote = quotes.get(i);
            int column = 5 + i;
            switch (metric) {
                case SUBTOTAL -> numeric(row, column, quote.subtotalBeforeTax(), styles.money);
                case TAX -> numeric(row, column, quote.taxAmount(), styles.money);
                case SHIPPING -> numeric(row, column, quote.shippingCost(), styles.money);
                case OTHER -> numeric(row, column, quote.otherCost(), styles.money);
                case TOTAL -> numeric(row, column, quote.totalAfterTax(), styles.total);
                case DELIVERY -> numeric(row, column, quote.deliveryDays() == null ? null : BigDecimal.valueOf(quote.deliveryDays()), styles.center);
                case PAYMENT -> cell(row, column, text(quote.paymentTerms()), styles.body);
                case WARRANTY -> cell(row, column, text(quote.warranty()), styles.body);
                case VALIDITY -> cell(row, column, quote.validUntil() == null ? "—" : DATE.format(quote.validUntil()), styles.date);
                case TECHNICAL -> cell(row, column, technicalLabel(quote.technicalStatus() == null ? "" : quote.technicalStatus().name()), styles.center);
                case SCORE -> numeric(row, column, quote.totalScore(), styles.score);
                case RANK -> numeric(row, column, quote.rank() == null ? null : BigDecimal.valueOf(quote.rank()), styles.center);
            }
        }
    }

    private List<RfqQuoteRoundResponse> latestRounds(List<RfqQuoteRoundResponse> rounds) {
        if (rounds == null || rounds.isEmpty()) return List.of();
        Map<UUID, RfqQuoteRoundResponse> latest = new LinkedHashMap<>();
        rounds.stream()
                .sorted(Comparator.comparingInt(RfqQuoteRoundResponse::roundNumber))
                .forEach(round -> latest.put(round.supplierId(), round));
        return new ArrayList<>(latest.values());
    }

    private CellStyle statusStyle(SheetStyles styles, String status) {
        return switch (status) {
            case "DRAFT", "RETURNED" -> styles.statusNeutral;
            case "PENDING_RFQ_APPROVAL", "PENDING_RESULT_APPROVAL" -> styles.statusPending;
            case "SENT", "RECEIVING_QUOTES", "COMPARING" -> styles.statusBlue;
            case "SELECTED_SUPPLIER", "PO_CREATED" -> styles.statusSuccess;
            case "REJECTED", "CANCELED", "EXPIRED" -> styles.statusMuted;
            default -> styles.statusNeutral;
        };
    }

    private void title(Sheet sheet, int rowIndex, int lastColumn,
                       String value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(rowIndex == 0 ? 30 : 23);
        Cell cell = row.createCell(0);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        if (lastColumn > 0) {
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, lastColumn));
            for (int column = 1; column <= lastColumn; column++) row.createCell(column).setCellStyle(style);
        }
    }

    private void cell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null || value.isBlank() ? "—" : value);
        cell.setCellStyle(style);
    }

    private void numeric(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value == null) cell.setCellValue("—");
        else cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private String dateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "—" : value;
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
            default -> "—";
        };
    }

    private enum QuoteMetric {
        SUBTOTAL, TAX, SHIPPING, OTHER, TOTAL, DELIVERY, PAYMENT, WARRANTY, VALIDITY, TECHNICAL, SCORE, RANK
    }

    private static final class SheetStyles {
        private final CellStyle title;
        private final CellStyle subtitle;
        private final CellStyle filter;
        private final CellStyle summary;
        private final CellStyle header;
        private final CellStyle body;
        private final CellStyle bodyAlt;
        private final CellStyle code;
        private final CellStyle center;
        private final CellStyle date;
        private final CellStyle money;
        private final CellStyle total;
        private final CellStyle score;
        private final CellStyle summaryLabel;
        private final CellStyle lowest;
        private final CellStyle highest;
        private final CellStyle missing;
        private final CellStyle overBudget;
        private final CellStyle statusNeutral;
        private final CellStyle statusPending;
        private final CellStyle statusBlue;
        private final CellStyle statusSuccess;
        private final CellStyle statusMuted;

        private SheetStyles(XSSFWorkbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.WHITE.getIndex());
            title = style(workbook, titleFont, "17324D", HorizontalAlignment.LEFT, true, "");

            Font subtitleFont = workbook.createFont();
            subtitleFont.setFontHeightInPoints((short) 10);
            subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subtitle = style(workbook, subtitleFont, "F3F6F9", HorizontalAlignment.LEFT, true, "");

            Font filterFont = workbook.createFont();
            filterFont.setFontHeightInPoints((short) 10);
            filterFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            filter = style(workbook, filterFont, "EAF0F6", HorizontalAlignment.LEFT, true, "");

            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            summary = style(workbook, summaryFont, "F6F8FA", HorizontalAlignment.LEFT, true, "");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 9);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            header = style(workbook, headerFont, "243B53", HorizontalAlignment.CENTER, true, "");

            Font bodyFont = workbook.createFont();
            bodyFont.setFontHeightInPoints((short) 10);
            body = style(workbook, bodyFont, "FFFFFF", HorizontalAlignment.LEFT, false, "");
            bodyAlt = style(workbook, bodyFont, "F7F9FB", HorizontalAlignment.LEFT, false, "");

            Font codeFont = workbook.createFont();
            codeFont.setBold(true);
            codeFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            code = style(workbook, codeFont, "EDF2F7", HorizontalAlignment.LEFT, false, "");
            center = style(workbook, bodyFont, "FFFFFF", HorizontalAlignment.CENTER, false, "");
            date = style(workbook, bodyFont, "FFFFFF", HorizontalAlignment.CENTER, false, "dd/mm/yyyy hh:mm");
            money = style(workbook, bodyFont, "FFFFFF", HorizontalAlignment.RIGHT, false, "#,##0.00 [$₫-vi-VN]");

            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalFont.setColor(IndexedColors.DARK_GREEN.getIndex());
            total = style(workbook, totalFont, "E8F5EC", HorizontalAlignment.RIGHT, false, "#,##0.00 [$₫-vi-VN]");

            Font scoreFont = workbook.createFont();
            scoreFont.setBold(true);
            score = style(workbook, scoreFont, "EEF3F8", HorizontalAlignment.CENTER, false, "0.00");
            summaryLabel = style(workbook, bodyFont, "F1F4F7", HorizontalAlignment.LEFT, true, "");
            lowest = style(workbook, bodyFont, "E4F3E8", HorizontalAlignment.RIGHT, false, "#,##0.00 [$₫-vi-VN]");
            highest = style(workbook, bodyFont, "FCE8E6", HorizontalAlignment.RIGHT, false, "#,##0.00 [$₫-vi-VN]");
            missing = style(workbook, bodyFont, "EEF0F2", HorizontalAlignment.CENTER, false, "");
            overBudget = style(workbook, bodyFont, "FFF1D6", HorizontalAlignment.RIGHT, false, "#,##0.00 [$₫-vi-VN]");
            statusNeutral = style(workbook, bodyFont, "F1F3F5", HorizontalAlignment.CENTER, true, "");
            statusPending = style(workbook, bodyFont, "FFF1D6", HorizontalAlignment.CENTER, true, "");
            statusBlue = style(workbook, bodyFont, "EAF2FA", HorizontalAlignment.CENTER, true, "");
            statusSuccess = style(workbook, bodyFont, "E4F3E8", HorizontalAlignment.CENTER, true, "");
            statusMuted = style(workbook, bodyFont, "FCE8E6", HorizontalAlignment.CENTER, true, "");
        }

        private static CellStyle style(XSSFWorkbook workbook, Font font, String fill, HorizontalAlignment alignment, boolean bold, String format) {
            CellStyle style = workbook.createCellStyle();
            style.setFont(font);
            style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.valueOf(fillColor(fill)).getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setAlignment(alignment);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setWrapText(true);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            if (bold && !font.getBold()) {
                Font emphasizedFont = workbook.createFont();
                emphasizedFont.setBold(true);
                emphasizedFont.setFontName(font.getFontName());
                emphasizedFont.setFontHeight(font.getFontHeight());
                emphasizedFont.setColor(font.getColor());
                style.setFont(emphasizedFont);
            }
            if (format != null && !format.isBlank()) style.setDataFormat(workbook.createDataFormat().getFormat(format));
            return style;
        }

        private static String fillColor(String hex) {
            return switch (hex) {
                case "17324D" -> "DARK_BLUE";
                case "F3F6F9", "F6F8FA", "F7F9FB", "F1F4F7", "EEF3F8", "EDF2F7", "EEF0F2", "F1F3F5" -> "GREY_25_PERCENT";
                case "EAF0F6", "EAF2FA" -> "PALE_BLUE";
                case "243B53" -> "DARK_BLUE";
                case "E8F5EC", "E4F3E8" -> "LIGHT_GREEN";
                case "FCE8E6" -> "ROSE";
                case "FFF1D6" -> "LIGHT_YELLOW";
                default -> "WHITE";
            };
        }
    }
}
