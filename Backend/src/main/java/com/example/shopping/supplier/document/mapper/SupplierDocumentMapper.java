package com.example.shopping.supplier.document.mapper;

import com.example.shopping.supplier.document.dto.response.SupplierDocumentDto;
import com.example.shopping.supplier.document.entity.SupplierDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupplierDocumentMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "filename", target = "filename")
    @Mapping(source = "contentType", target = "contentType")
    @Mapping(source = "size", target = "size")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "submittedBy", target = "submittedBy")
    @Mapping(source = "submittedAt", target = "submittedAt")
    @Mapping(source = "reviewedBy", target = "reviewedBy")
    @Mapping(source = "reviewedAt", target = "reviewedAt")
    @Mapping(source = "reviewComment", target = "reviewComment")
    SupplierDocumentDto toDto(SupplierDocument document);
}