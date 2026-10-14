package com.example.shopping.supplier.contract.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.example.shopping.supplier.contract.dto.request.SupplierContractRequest;
import com.example.shopping.supplier.contract.dto.response.SupplierContractDto;
import com.example.shopping.supplier.contract.dto.response.SupplierContractDownloadDto;

public interface SupplierContractService {

    List<SupplierContractDto> list();

    SupplierContractDto create(SupplierContractRequest request, MultipartFile file) throws IOException;

    SupplierContractDto update(UUID id, SupplierContractRequest request, MultipartFile file) throws IOException;

    void delete(UUID id);

    SupplierContractDownloadDto download(UUID id);
}
