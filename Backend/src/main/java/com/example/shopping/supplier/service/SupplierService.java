package com.example.shopping.supplier.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.shopping.supplier.dto.request.CreateSupplierRequestDto;
import com.example.shopping.supplier.dto.request.UpdateSupplierRequestDto;
import com.example.shopping.supplier.dto.response.SupplierResponseDto;

public interface SupplierService {

    List<SupplierResponseDto> getAllSuppliers();

    Optional<SupplierResponseDto> getSupplierById(UUID id);

    Optional<SupplierResponseDto> getSupplierByCode(String code);

    List<SupplierResponseDto> searchSuppliers(String keyword);

    SupplierResponseDto createSupplier(CreateSupplierRequestDto request);

    SupplierResponseDto updateSupplier(UUID id, UpdateSupplierRequestDto request);

    void deleteSupplier(UUID id);
}
