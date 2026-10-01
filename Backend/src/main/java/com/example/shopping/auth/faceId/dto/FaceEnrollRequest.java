package com.example.shopping.auth.faceId.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class FaceEnrollRequest {

    @NotBlank(message = "username is required")
    private String username;

    /**
     * Single base64 image (optional if images list is provided).
     */
    private String image;

    /**
     * Multiple base64 images representing different angles (straight, left, right).
     */
    private List<String> images;
}
