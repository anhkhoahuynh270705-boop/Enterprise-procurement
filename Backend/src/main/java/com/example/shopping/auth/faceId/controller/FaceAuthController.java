package com.example.shopping.auth.faceId.controller;

import com.example.shopping.auth.dto.response.LoginResponseDto;
import com.example.shopping.auth.faceId.dto.FaceEnrollRequest;
import com.example.shopping.auth.faceId.dto.FaceLoginRequest;
import com.example.shopping.auth.faceId.service.FaceAuthService;
import com.example.shopping.auth.helper.AuthHttpHelper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/face")
@RequiredArgsConstructor
public class FaceAuthController {

    private final FaceAuthService faceAuthService;
    private final AuthHttpHelper authHttpHelper;

    /**
     * Enroll a face for the given username.
     * Supports single or multi-pose image capture.
     */
    @PostMapping("/enroll")
    public ResponseEntity<Map<String, String>> enroll(@Valid @RequestBody FaceEnrollRequest req) {
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            faceAuthService.enroll(req.getUsername(), req.getImages());
        } else {
            faceAuthService.enroll(req.getUsername(), req.getImage());
        }
        return ResponseEntity.ok(Map.of("message", "Khuôn mặt đã được đăng ký thành công"));
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> faceLogin(@Valid @RequestBody FaceLoginRequest req) {
        LoginResponseDto tokens = faceAuthService.verifyAndLogin(req.getUsername(), req.getImage());
        return authHttpHelper.tokenResponse(tokens);
    }

    /**
     * Check if a user has an enrolled face template.
     */
    @GetMapping("/enrolled")
    public ResponseEntity<Map<String, Boolean>> isEnrolled(@RequestParam String username) {
        boolean enrolled = faceAuthService.isEnrolled(username);
        return ResponseEntity.ok(Map.of("enrolled", enrolled));
    }
}
