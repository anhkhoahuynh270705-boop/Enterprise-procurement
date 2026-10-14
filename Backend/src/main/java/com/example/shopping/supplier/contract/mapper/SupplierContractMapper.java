package com.example.shopping.supplier.contract.mapper;

import java.time.LocalDate;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.example.shopping.supplier.contract.dto.response.SupplierContractDto;
import com.example.shopping.supplier.contract.entity.SupplierContract;
import com.example.shopping.supplier.contract.enums.SupplierContractStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SupplierContractMapper {

    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.code", target = "supplierCode")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(target = "status", expression = "java(resolveStatus(contract))")
    SupplierContractDto toDto(SupplierContract contract);

    default SupplierContractStatus resolveStatus(SupplierContract contract) {
        if (contract.getStatus() == SupplierContractStatus.ACTIVE
                && contract.getEndDate().isBefore(LocalDate.now())) {
            return SupplierContractStatus.EXPIRED;
        }
        return contract.getStatus();
    }
}
