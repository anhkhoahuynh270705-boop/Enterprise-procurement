package com.example.shopping.supplier.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.security.TicketAccess;
import com.example.shopping.supplier.dto.response.SupplierResponseDto;
import com.example.shopping.supplier.mapper.SupplierMapper;
import com.example.shopping.supplier.repository.SupplierRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierCache {

    static final String CACHE_NAME = "suppliers";

    SupplierRepository supplierRepository;
    SupplierMapper supplierMapper;

    @Cacheable(cacheNames = CACHE_NAME,
            key = "'all:' + T(com.example.shopping.security.TicketAccess).cacheScope()")
    @Transactional(readOnly = true)
    public List<SupplierResponseDto> getAll() {
        return supplierRepository.findAll().stream().map(supplierMapper::toDto).toList();
    }

    @Cacheable(cacheNames = CACHE_NAME,
            key = "'id:' + #id.toString() + ':' + T(com.example.shopping.security.TicketAccess).cacheScope()")
    @Transactional(readOnly = true)
    public Optional<SupplierResponseDto> getById(UUID id) {
        return supplierRepository.findById(id).map(supplierMapper::toDto);
    }

    @Cacheable(cacheNames = CACHE_NAME,
            key = "'code:' + #code + ':' + T(com.example.shopping.security.TicketAccess).cacheScope()")
    @Transactional(readOnly = true)
    public Optional<SupplierResponseDto> getByCode(String code) {
        return supplierRepository.findByCode(code).map(supplierMapper::toDto);
    }

    @Cacheable(cacheNames = CACHE_NAME,
            key = "'search:' + #keyword.trim().toLowerCase() + ':' + T(com.example.shopping.security.TicketAccess).cacheScope()")
    @Transactional(readOnly = true)
    public List<SupplierResponseDto> search(String keyword) {
        return supplierRepository.searchByKeyword(keyword.trim()).stream()
                .map(supplierMapper::toDto).toList();
    }
}
