package com.example.shopping.auth.faceId.controller;

import com.example.shopping.auth.dto.response.LoginResponseDto;
import com.example.shopping.auth.faceId.dto.request.FaceEnrollRequest;
import com.example.shopping.auth.faceId.dto.request.FaceLoginRequest;
import com.example.shopping.auth.faceId.service.FaceAuthService;
import com.example.shopping.auth.helper.AuthHttpHelper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@RestController
@RequestMapping("/api/face")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FaceAuthController {

    FaceAuthService faceAuthService;
    AuthHttpHelper authHttpHelper;

    /**
     * Enroll a face for the given username.
     * Supports single or multi-pose image capture.
     */
    @PostMapping("/enroll")
public ResponseEntity<Map<String, String>> enroll(@Valid @RequestBody FaceEnrollRequest req) {
    faceAuthService.enroll(req.getUsername(), req.getImages());
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
