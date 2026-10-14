package com.example.shopping.auth.faceId.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class FaceEnrollRequest {

    @NotBlank(message = "username is required")
    private String username;
    private List<String> images;
}
