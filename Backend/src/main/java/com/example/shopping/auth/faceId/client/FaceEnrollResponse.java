package com.example.shopping.auth.faceId.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response payload from Python Face Service for /face/enroll endpoint.
 */
@Data
public class FaceEnrollResponse {

    private boolean success;

    private String message;

    @JsonProperty("enrolled_poses")
    private int enrolledPoses = 1;
}
