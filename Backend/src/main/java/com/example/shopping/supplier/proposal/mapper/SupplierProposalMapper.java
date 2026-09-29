package com.example.shopping.supplier.proposal.mapper;

import com.example.shopping.supplier.proposal.dto.response.SupplierProposalResponseDto;
import com.example.shopping.supplier.proposal.entity.SupplierProposal;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SupplierProposalMapper {
    SupplierProposalResponseDto toDto(SupplierProposal proposal);
}
