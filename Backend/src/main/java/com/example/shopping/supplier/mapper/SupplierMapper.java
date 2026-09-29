package com.example.shopping.supplier.mapper;

import org.mapstruct.*;
import com.example.shopping.supplier.dto.request.CreateSupplierRequestDto;
import com.example.shopping.supplier.dto.request.UpdateSupplierRequestDto;
import com.example.shopping.supplier.dto.response.SupplierResponseDto;
import com.example.shopping.supplier.entity.SupplierEntity;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true),
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SupplierMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "contactPerson", source = "dto.contactPerson")
    @Mapping(target = "email", source = "dto.email")
    @Mapping(target = "phone", source = "dto.phone")
    @Mapping(target = "address", source = "dto.address")
    @Mapping(target = "city", source = "dto.city")
    @Mapping(target = "country", source = "dto.country")
    @Mapping(target = "taxCode", source = "dto.taxCode")
    @Mapping(target = "bankAccount", source = "dto.bankAccount")
    @Mapping(target = "bankName", source = "dto.bankName")
    @Mapping(target = "notes", source = "dto.notes")
    SupplierEntity toEntity(CreateSupplierRequestDto dto, String code);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", source = "status",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "contactPerson", source = "contactPerson")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "taxCode", source = "taxCode")
    @Mapping(target = "bankAccount", source = "bankAccount")
    @Mapping(target = "bankName", source = "bankName")
    @Mapping(target = "notes", source = "notes")
    void updateEntity(@MappingTarget SupplierEntity entity, UpdateSupplierRequestDto dto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "contactPerson", source = "contactPerson")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "address", source = "address")
    @Mapping(target = "city", source = "city")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "taxCode", source = "taxCode")
    @Mapping(target = "bankAccount", source = "bankAccount")
    @Mapping(target = "bankName", source = "bankName")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    SupplierResponseDto toDto(SupplierEntity entity);
}