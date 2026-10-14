package com.example.shopping.company.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.example.shopping.company.dto.CompanyDocumentDto;
import com.example.shopping.company.entity.CompanyDocument;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CompanyDocumentMapper {

    CompanyDocumentDto toDto(CompanyDocument document);
}
