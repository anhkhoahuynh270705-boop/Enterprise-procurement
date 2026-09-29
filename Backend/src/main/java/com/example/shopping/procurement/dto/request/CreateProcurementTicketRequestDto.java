package com.example.shopping.procurement.dto.request;

import java.util.ArrayList;
import java.util.List;

import com.example.shopping.procurement.dto.request.ProcurementItemRequestDto;
import com.example.shopping.procurement.enums.ProcurementPriority;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class CreateProcurementTicketRequestDto {

    @NotBlank(message = "Tiêu đề yêu cầu mua sắm không được để trống")
    private String title;

    private String department;

    @NotBlank(message= "Lý do mua sắm không được để trống")
    private String reason;

    @Builder.Default
    private String currency = "VND";

    @Builder.Default
    private ProcurementPriority priority = ProcurementPriority.MEDIUM;

    private boolean submitImmediately;

    @NotEmpty(message = "Danh sách sản phẩm mua sắm không được để trống")
    @Valid
    @Builder.Default
    private List<ProcurementItemRequestDto> items = new ArrayList<>();
}
