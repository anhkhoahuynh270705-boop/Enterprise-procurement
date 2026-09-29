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
    @Mapping(target = "ticketCode", source = "ticketCode")
    @Mapping(target = "makerUsername", source = "makerUsername")
    @Mapping(target = "currency", source = "dto.currency", defaultValue = "VND")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "title", source = "dto.title")
    @Mapping(target = "department", source = "dto.department")
    @Mapping(target = "reason", source = "dto.reason")
    @Mapping(target = "priority", source = "dto.priority")
    @Mapping(target = "items", source = "dto.items")
    ProcurementTicket toEntity(CreateProcurementTicketRequestDto dto, String ticketCode, String makerUsername);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "quantity", source = "quantity", defaultValue = "1")
    @Mapping(target = "unitPrice", source = "unitPrice", defaultValue = "0")
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "itemCode", source = "itemCode")
    @Mapping(target = "itemName", source = "itemName")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "unit", source = "unit")
    @Mapping(target = "supplierName", source = "supplierName")
    @Mapping(target = "notes", source = "notes")
    ProcurementItem toItemEntity(ProcurementItemRequestDto dto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "ticketCode", source = "ticketCode")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "department", source = "department")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "priority", source = "priority")
    @Mapping(target = "makerUsername", source = "makerUsername")
    @Mapping(target = "checkerUsername", source = "checkerUsername")
    @Mapping(target = "checkerComment", source = "checkerComment")
    @Mapping(target = "camundaProcessInstanceId", source = "camundaProcessInstanceId")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "approvedAt", source = "approvedAt")
    @Mapping(target = "camundaTaskId", ignore = true)
    ProcurementTicketResponseDto toDto(ProcurementTicket entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "totalPrice", source = "totalPrice")
    @Mapping(target = "itemCode", source = "itemCode")
    @Mapping(target = "itemName", source = "itemName")
    @Mapping(target = "category", source = "category")
    @Mapping(target = "unit", source = "unit")
    @Mapping(target = "supplierName", source = "supplierName")
    @Mapping(target = "notes", source = "notes")
    ProcurementItemDto toItemDto(ProcurementItem entity);

    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<ProcurementItemDto> toItemDtos(List<ProcurementItem> items);

    @AfterMapping
    default void linkItems(@MappingTarget ProcurementTicket ticket) {
        if (ticket.getItems() != null) ticket.getItems().forEach(item -> item.setTicket(ticket));
    }
}
