package com.example.shopping.supplier.proposal.controller;

import com.example.shopping.supplier.proposal.dto.request.SupplierProposalRequest;
import com.example.shopping.supplier.proposal.dto.request.SupplierProposalReviewRequest;
import com.example.shopping.supplier.proposal.dto.response.SupplierProposalResponseDto;
import com.example.shopping.supplier.proposal.service.SupplierProposalService;

import java.util.List;
import com.example.shopping.common.dto.response.CountResponseDto;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/supplier-proposals")
@RequiredArgsConstructor
public class SupplierProposalController {
    private final SupplierProposalService service;

    @GetMapping
    public List<SupplierProposalResponseDto> list() { 
        return service.list(); 
    }

    @GetMapping("/pending-count")
    public CountResponseDto pendingCount() { 
        return new CountResponseDto(service.pendingCount()); 
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierProposalResponseDto submit(@Valid @RequestBody SupplierProposalRequest request) {
        return service.submit(request);
    }

    @PostMapping("/{id}/review")
    public SupplierProposalResponseDto review(@PathVariable UUID id, @Valid @RequestBody SupplierProposalReviewRequest request) {
        return service.review(id, request);
    }
}
