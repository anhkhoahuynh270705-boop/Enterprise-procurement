package com.example.shopping.supplier.document.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SupplierDocumentReviewRequest(
    @NotNull(message = "Vui lòng chọn kết quả duyệt") Boolean approved,
    @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự") String comment
) {
    public SupplierDocumentReviewRequest {
        comment = comment == null ? "" : comment.strip();
    }

    @JsonIgnore
    @AssertTrue(message = "Vui lòng nhập lý do từ chối")
    public boolean isRejectionReasonProvided() {
        return !Boolean.FALSE.equals(approved) || !comment.isBlank();
    }
}
