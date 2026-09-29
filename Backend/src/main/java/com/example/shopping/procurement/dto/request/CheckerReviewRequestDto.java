package com.example.shopping.procurement.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckerReviewRequestDto {

    @NotNull(message = "Kết quả duyệt không được để trống")
    private Boolean approved;

    private String comment;
}
