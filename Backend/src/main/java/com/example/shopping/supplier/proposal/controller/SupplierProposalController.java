package com.example.shopping.supplier.proposal.controller;

import com.example.shopping.supplier.proposal.dto.request.SupplierProposalRequest;
import com.example.shopping.supplier.proposal.dto.request.SupplierProposalReviewRequest;
import com.example.shopping.supplier.proposal.dto.request.SupplierProposalSearchCriteria;
import com.example.shopping.supplier.proposal.dto.response.SupplierProposalResponseDto;
import com.example.shopping.supplier.proposal.service.SupplierProposalService;
import com.example.shopping.supplier.proposal.enums.SupplierProposalStatus;

import java.util.List;
import com.example.shopping.common.dto.response.CountResponseDto;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/supplier-proposals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierProposalController {
    SupplierProposalService service;

    @GetMapping
    public List<SupplierProposalResponseDto> list() { 
        return service.list(); 
    }

    @GetMapping("/page")
    public Page<SupplierProposalResponseDto> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SupplierProposalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.search(new SupplierProposalSearchCriteria(keyword, status), PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "submittedAt").and(Sort.by(Sort.Direction.DESC, "id"))));
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
