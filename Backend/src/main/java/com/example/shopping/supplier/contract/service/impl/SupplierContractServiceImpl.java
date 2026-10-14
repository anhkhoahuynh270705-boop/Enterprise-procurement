package com.example.shopping.supplier.contract.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.security.TicketAccess;
import com.example.shopping.supplier.contract.dto.request.SupplierContractRequest;
import com.example.shopping.supplier.contract.dto.response.SupplierContractDto;
import com.example.shopping.supplier.contract.dto.response.SupplierContractDownloadDto;
import com.example.shopping.supplier.contract.entity.SupplierContract;
import com.example.shopping.supplier.contract.entity.SupplierContractContent;
import com.example.shopping.supplier.contract.enums.SupplierContractStatus;
import com.example.shopping.supplier.contract.mapper.SupplierContractMapper;
import com.example.shopping.supplier.contract.repository.SupplierContractContentRepository;
import com.example.shopping.supplier.contract.repository.SupplierContractRepository;
import com.example.shopping.supplier.contract.service.SupplierContractService;
import com.example.shopping.supplier.contract.service.SupplierContractNumberGenerator;
import com.example.shopping.supplier.document.model.ValidatedSupplierDocument;
import com.example.shopping.supplier.document.validation.SupplierDocumentValidator;
import com.example.shopping.supplier.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SupplierContractServiceImpl implements SupplierContractService {

    SupplierContractRepository contracts;
    SupplierContractContentRepository contents;
    SupplierRepository suppliers;
    SupplierContractNumberGenerator contractNumbers;
    SupplierContractMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<SupplierContractDto> list() {
        requireReader();
        return contracts.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional
    public SupplierContractDto create(SupplierContractRequest request, MultipartFile file) throws IOException {
        requireAdmin();
        validateRequest(request);
        var supplier = suppliers.findById(request.getSupplierId())
                .orElseThrow(() -> ErrorEnum.SUPPLIER_NOT_FOUND.ex());
        ValidatedSupplierDocument validatedFile = file == null || file.isEmpty()
                ? null
                : SupplierDocumentValidator.validate(file);

        SupplierContract contract = new SupplierContract();
        apply(contract, request, supplier);
        contract.setContractNumber(contractNumbers.nextFor(Year.now().getValue()));
        contract.setCreatedBy(TicketAccess.username());
        contract.setCreatedAt(LocalDateTime.now());
        contract = contracts.saveAndFlush(contract);
        if (validatedFile != null)
            saveContent(contract, validatedFile);
        return mapper.toDto(contract);
    }

    @Override
    @Transactional
    public SupplierContractDto update(UUID id, SupplierContractRequest request, MultipartFile file) throws IOException {
        requireAdmin();
        validateRequest(request);
        SupplierContract contract = contracts.findWithSupplierById(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_CONTRACT_NOT_FOUND.ex());
        var supplier = suppliers.findById(request.getSupplierId())
                .orElseThrow(() -> ErrorEnum.SUPPLIER_NOT_FOUND.ex());
        ValidatedSupplierDocument validatedFile = file == null || file.isEmpty()
                ? null
                : SupplierDocumentValidator.validate(file);

        apply(contract, request, supplier);
        contract.setUpdatedAt(LocalDateTime.now());
        contract = contracts.saveAndFlush(contract);
        if (validatedFile != null) saveContent(contract, validatedFile);
        return mapper.toDto(contract);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        requireAdmin();
        SupplierContract contract = contracts.findById(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_CONTRACT_NOT_FOUND.ex());
        contents.deleteById(id);
        contracts.delete(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierContractDownloadDto download(UUID id) {
        requireReader();
        SupplierContract contract = contracts.findById(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_CONTRACT_NOT_FOUND.ex());
        SupplierContractContent content = contents.findById(id)
                .orElseThrow(() -> ErrorEnum.SUPPLIER_CONTRACT_NO_ATTACHMENT.ex());
        return new SupplierContractDownloadDto(
                contract.getAttachmentName(),
                contract.getAttachmentContentType(),
                content.getBytes());
    }

    private void apply(
            SupplierContract contract,
            SupplierContractRequest request,
            com.example.shopping.supplier.entity.SupplierEntity supplier) {
        contract.setTitle(request.getTitle().trim());
        contract.setSupplier(supplier);
        contract.setSignedDate(request.getSignedDate());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setAmount(request.getAmount());
        contract.setCurrency(request.getCurrency().trim().toUpperCase(Locale.ROOT));
        contract.setStatus(request.getStatus());
        contract.setNotes(optionalText(request.getNotes()));
    }

    private void saveContent(SupplierContract contract, ValidatedSupplierDocument file) {
        SupplierContractContent content = contents.findById(contract.getId()).orElseGet(() -> {
            SupplierContractContent created = new SupplierContractContent();
            created.setContract(contract);
            return created;
        });
        content.setBytes(file.bytes());
        contents.save(content);
        contract.setAttachmentName(file.filename());
        contract.setAttachmentContentType(file.contentType());
    }

    private void validateRequest(SupplierContractRequest request) {
        if (request.getTitle() != null && request.getTitle().trim().length() < 2) {
            throw ErrorEnum.SUPPLIER_CONTRACT_INVALID_TITLE.ex();
        }
        if (request.getStatus() == SupplierContractStatus.EXPIRED) {
            throw ErrorEnum.SUPPLIER_CONTRACT_EXPIRED_MANUAL.ex();
        }
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw ErrorEnum.SUPPLIER_CONTRACT_INVALID_DATES.ex();
        }
        if (request.getSignedDate() != null && request.getStartDate() != null
                && request.getSignedDate().isAfter(request.getStartDate())) {
            throw ErrorEnum.SUPPLIER_CONTRACT_SIGNED_DATE_INVALID.ex();
        }
    }

    private void requireReader() {
        if (!TicketAccess.hasRole("ADMIN") && !TicketAccess.hasRole("CHECKER")) {
            throw ErrorEnum.ACCESS_DENIED.ex("Chỉ Admin hoặc Checker được xem hợp đồng nhà cung cấp.");
        }
    }

    private void requireAdmin() {
        if (!TicketAccess.hasRole("ADMIN")) {
            throw ErrorEnum.ACCESS_DENIED.ex("Chỉ Admin được quản lý hợp đồng nhà cung cấp.");
        }
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
