package com.example.shopping.supplier.proposal.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;
import com.example.shopping.supplier.proposal.enums.SupplierProposalStatus;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SupplierProposalResponseDto {
    private UUID id;
    private String name;
    private String taxCode;
    private String contactPerson;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
    private String bankName;
    private String bankAccount;
    private String notes;
    private String reason;
    private SupplierProposalStatus status;
    private String submittedBy;
    private LocalDateTime submittedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private UUID supplierId;
}
