package com.example.shopping.auth.faceId.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaceLoginRequest {

    /**
     * Optional username. If provided, does fast 1:1 verification.
     * If blank/null, does 1:N automatic user identification.
     */
    private String username;

    @NotBlank(message = "image is required")
    private String image;  
}
