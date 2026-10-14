package com.example.shopping.procurement.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.*;
import com.example.shopping.procurement.dto.request.CreateProcurementTicketRequestDto;
import com.example.shopping.procurement.dto.request.ProcurementItemRequestDto;
import com.example.shopping.procurement.dto.response.ProcurementItemDto;
import com.example.shopping.procurement.dto.response.ProcurementTicketResponseDto;
import com.example.shopping.procurement.entity.ProcurementItem;
import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.enums.ProcurementStatus;

@Mapper(componentModel = "spring", imports = {BigDecimal.class, ProcurementStatus.class},
    builder = @Builder(disableBuilder = true), unmappedTargetPolicy = ReportingPolicy.ERROR,
    collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface ProcurementMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "checkerUsername", ignore = true)
    @Mapping(target = "checkerComment", ignore = true)
    @Mapping(target = "camundaProcessInstanceId", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "currency", source = "dto.currency", defaultValue = "VND")
    @Mapping(target = "status", ignore = true)
    ProcurementTicket toEntity(CreateProcurementTicketRequestDto dto, String ticketCode, String makerUsername);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "quantity", source = "quantity", defaultValue = "1")
    @Mapping(target = "unitPrice", source = "unitPrice", defaultValue = "0")
    @Mapping(target = "totalPrice", ignore = true)
    ProcurementItem toItemEntity(ProcurementItemRequestDto dto);

    @Mapping(target = "camundaTaskId", ignore = true)
    ProcurementTicketResponseDto toDto(ProcurementTicket entity);
    ProcurementItemDto toItemDto(ProcurementItem entity);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<ProcurementItemDto> toItemDtos(List<ProcurementItem> items);

    @AfterMapping
    default void linkItems(@MappingTarget ProcurementTicket ticket) {
        if (ticket.getItems() != null)
            ticket.getItems().forEach(item -> item.setTicket(ticket));
    }
}
