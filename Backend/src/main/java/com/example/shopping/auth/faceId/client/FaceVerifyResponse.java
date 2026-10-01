package com.example.shopping.auth.faceId.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response payload from the Python Face Service for /face/verify endpoint.
 */
@Data
public class FaceVerifyResponse {

    private boolean verified;

    private double similarity;

    @JsonProperty("user_id")
    private String userId;

    private String message;
}
