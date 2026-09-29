package com.example.shopping.supplier.proposal.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;

public record SupplierProposalReviewRequest(
    @NotNull(message = "Vui lòng chọn kết quả duyệt") Boolean approved,
    @Size(max = 2000, message = "Nhận xét tối đa 2000 ký tự") String comment
) {
    public SupplierProposalReviewRequest { 
        comment = SupplierProposalRequest.clean(comment); 
    }

    @JsonIgnore
    @AssertTrue(message = "Vui lòng nhập lý do từ chối")
    public boolean isRejectionReasonProvided() { 
        return !Boolean.FALSE.equals(approved) || !comment.isBlank(); 
    }
}
