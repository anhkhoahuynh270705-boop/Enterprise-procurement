package com.example.shopping.procurement.dto.request;

import java.util.List;

import com.example.shopping.procurement.dto.request.ProcurementItemRequestDto;
import com.example.shopping.procurement.enums.ProcurementPriority;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
public class UpdateProcurementTicketRequestDto {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Phòng ban không được để trống")
    private String department;  

    @NotBlank(message = "Lý do không được để trống")
    private String reason;

    private ProcurementPriority priority;

    @Valid
    private List<ProcurementItemRequestDto> items;
}
