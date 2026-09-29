package com.example.shopping.procurement.dto.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.shopping.procurement.enums.ProcurementPriority;
import com.example.shopping.procurement.enums.ProcurementStatus;

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
public class ProcurementTicketResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private String ticketCode;

    private String title;

    private String department;

    private String reason;

    private BigDecimal totalAmount;

    private String currency;

    private ProcurementStatus status;

    private ProcurementPriority priority;

    private String makerUsername;

    private String checkerUsername;

    private String checkerComment;

    private String camundaProcessInstanceId;

    private String camundaTaskId;

    @Builder.Default
    private List<ProcurementItemDto> items = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime approvedAt;
}
