package com.example.shopping.integration.cloudinary.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.shopping.integration.cloudinary.dto.UploadedImage;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryStorageService {

    Cloudinary cloudinary;

    public UploadedImage uploadImage(byte[] bytes, String folder, String publicId) throws IOException {
        Map<?, ?> response = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "resource_type", "image",
                "folder", folder,
                "public_id", publicId,
                "overwrite", true,
                "invalidate", true));

        Object secureUrl = response.get("secure_url");
        Object uploadedPublicId = response.get("public_id");
        if (!(secureUrl instanceof String url) || !(uploadedPublicId instanceof String id)) {
            throw new IOException("Cloudinary did not return the uploaded image URL and public ID.");
        }
        return new UploadedImage(url, id);
    }

    public void deleteImage(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) return;
        cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                "resource_type", "image",
                "invalidate", true));
    }

}
