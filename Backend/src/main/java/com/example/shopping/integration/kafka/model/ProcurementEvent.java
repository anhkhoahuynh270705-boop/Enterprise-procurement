package com.example.shopping.integration.kafka.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.shopping.integration.kafka.event.enums.ProcurementEventType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProcurementEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID ticketId;

    private String ticketCode;

    private String title;

    private String makerUsername;

    private String checkerUsername;

    private BigDecimal totalAmount;

    private String currency;

    private ProcurementEventType eventType;

    private String comment;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
