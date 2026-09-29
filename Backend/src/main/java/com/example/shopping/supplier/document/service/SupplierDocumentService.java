package com.example.shopping.supplier.document.service;

import com.example.shopping.supplier.document.dto.request.SupplierDocumentReviewRequest;
import com.example.shopping.supplier.document.dto.response.SupplierDocumentDto;
import com.example.shopping.supplier.document.dto.response.SupplierDocumentDownloadDto;
import com.example.shopping.supplier.document.dto.response.SupplierOptionDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface SupplierDocumentService {
    List<SupplierOptionDto> supplierOptions();
    List<SupplierDocumentDto> list();
    SupplierDocumentDto upload(UUID supplierId, MultipartFile file) throws IOException;
    SupplierDocumentDownloadDto download(UUID id);
    SupplierDocumentDto review(UUID id, SupplierDocumentReviewRequest request);
}
