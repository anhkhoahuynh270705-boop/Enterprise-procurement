package com.example.shopping.auth.faceId.service;

import com.example.shopping.auth.dto.response.LoginResponseDto;
import java.util.List;

/**
 * FaceAuthService - Orchestrates face enrollment and multi-angle authentication.
 */
public interface FaceAuthService {

    /**
     * Enroll single or multi-angle faces for a user (straight, left, right).
     */
    void enroll(String username, String imageB64);

    void enroll(String username, List<String> imagesB64);

    /**
     * Authenticate face against stored templates.
     * If username is provided, verifies 1:1.
     * If username is null/blank, identifies user 1:N.
     */
    LoginResponseDto verifyAndLogin(String username, String imageB64);

    /**
     * Returns true if the user has an enrolled face template.
     */
    boolean isEnrolled(String username);
}
