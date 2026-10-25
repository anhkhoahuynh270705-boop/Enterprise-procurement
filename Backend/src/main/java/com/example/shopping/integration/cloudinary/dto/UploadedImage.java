package com.example.shopping.integration.cloudinary.dto;

public record UploadedImage (
        String secureUrl,
        String publicId
) {}
