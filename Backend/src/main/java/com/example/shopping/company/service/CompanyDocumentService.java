package com.example.shopping.company.service;

import com.example.shopping.company.dto.CompanyDocumentDto;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface CompanyDocumentService {

    List<CompanyDocumentDto> list();

    CompanyDocumentDto upload(String title, String category, String description, MultipartFile file)
            throws IOException;

    Download download(UUID id);

    void delete(UUID id);

    record Download(String filename, String contentType, byte[] bytes) {
    }
}