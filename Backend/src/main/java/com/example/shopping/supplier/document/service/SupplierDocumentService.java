package com.example.shopping.supplier.document.service;

import com.example.shopping.supplier.document.dto.request.SupplierDocumentReviewRequest;
import com.example.shopping.supplier.document.dto.request.SupplierDocumentSearchCriteria;
import com.example.shopping.supplier.document.dto.response.SupplierDocumentDto;
import com.example.shopping.supplier.document.dto.response.SupplierDocumentDownloadDto;
import com.example.shopping.supplier.document.dto.response.SupplierOptionDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierDocumentService {
    List<SupplierOptionDto> supplierOptions();
    List<SupplierDocumentDto> list();
    Page<SupplierDocumentDto> search(SupplierDocumentSearchCriteria criteria, Pageable pageable);
    SupplierDocumentDto upload(UUID supplierId, MultipartFile file) throws IOException;
    SupplierDocumentDownloadDto download(UUID id);
    SupplierDocumentDto review(UUID id, SupplierDocumentReviewRequest request);
}
