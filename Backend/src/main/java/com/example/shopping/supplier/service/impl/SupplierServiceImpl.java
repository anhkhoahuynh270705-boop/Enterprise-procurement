package com.example.shopping.supplier.service.impl;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopping.common.enums.ErrorEnum;

import com.example.shopping.supplier.dto.request.CreateSupplierRequestDto;
import com.example.shopping.supplier.dto.request.UpdateSupplierRequestDto;
import com.example.shopping.supplier.dto.response.SupplierResponseDto;
import com.example.shopping.supplier.entity.SupplierEntity;
import com.example.shopping.supplier.mapper.SupplierMapper;
import com.example.shopping.supplier.repository.SupplierRepository;
import com.example.shopping.supplier.service.SupplierService;
import com.example.shopping.supplier.document.repository.SupplierDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierServiceImpl implements SupplierService {

    SupplierRepository supplierRepository;
    SupplierMapper supplierMapper;
    SupplierDocumentRepository documents;
    SupplierCache supplierCache;

    /**
     * Sinh mã nhà cung cấp tự động: SUP-YYYY-XXXX
     */
    private synchronized String generateNextCode() {
        int currentYear = Year.now().getValue();
        String prefix = "SUP-" + currentYear + "-";
        long count = supplierRepository.count() + 1;
        return String.format("SUP-%d-%04d", currentYear, count);
    }

    @Override
    public List<SupplierResponseDto> getAllSuppliers() {
        return supplierCache.getAll();
    }

    @Override
    public Optional<SupplierResponseDto> getSupplierById(UUID id) {
        return supplierCache.getById(id);
    }

    @Override
    public Optional<SupplierResponseDto> getSupplierByCode(String code) {
        return supplierCache.getByCode(code);
    }

    @Override
    public List<SupplierResponseDto> searchSuppliers(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllSuppliers();
        }
        return supplierCache.search(keyword.trim());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = SupplierCache.CACHE_NAME, allEntries = true)
    public SupplierResponseDto createSupplier(CreateSupplierRequestDto request) {
        if (request.getTaxCode() != null && !request.getTaxCode().isBlank()
                && supplierRepository.existsByTaxCode(request.getTaxCode())) {
            throw ErrorEnum.SUPPLIER_TAX_CODE_EXISTS.ex("Mã số thuế đã tồn tại: " + request.getTaxCode());
        }

        String code = generateNextCode();
        SupplierEntity entity = supplierMapper.toEntity(request, code);
        SupplierEntity saved = supplierRepository.save(entity);
        log.info("Đã tạo nhà cung cấp mới: [{}] - {}", saved.getCode(), saved.getName());
        return supplierMapper.toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = SupplierCache.CACHE_NAME, allEntries = true)
    public SupplierResponseDto updateSupplier(UUID id, UpdateSupplierRequestDto request) {
        SupplierEntity entity = supplierRepository.findById(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_NOT_FOUND.ex("Không tìm thấy nhà cung cấp với id: " + id));

        if (request.getTaxCode() != null && !request.getTaxCode().isBlank()
                && !request.getTaxCode().equals(entity.getTaxCode())
                && supplierRepository.existsByTaxCode(request.getTaxCode())) {
            throw ErrorEnum.SUPPLIER_TAX_CODE_EXISTS.ex("Mã số thuế đã tồn tại: " + request.getTaxCode());
        }

        supplierMapper.updateEntity(entity, request);
        SupplierEntity saved = supplierRepository.save(entity);
        log.info("Đã cập nhật nhà cung cấp: [{}] - {}", saved.getCode(), saved.getName());
        return supplierMapper.toDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = SupplierCache.CACHE_NAME, allEntries = true)
    public void deleteSupplier(UUID id) {
        SupplierEntity entity = supplierRepository.findById(id).orElse(null);
        if (entity == null) {
            log.warn("Nhà cung cấp id {} không tồn tại", id);
            return;
        }
        if (documents.existsBySupplier_Id(id)) {
            throw ErrorEnum.DATA_CONFLICT.ex(
                    "Nhà cung cấp đang có tài liệu. Hãy chuyển sang trạng thái INACTIVE thay vì xóa.");
        }
        supplierRepository.deleteById(id);
        log.info("Đã xóa nhà cung cấp: [{}] - {}", entity.getCode(), entity.getName());
    }
}
