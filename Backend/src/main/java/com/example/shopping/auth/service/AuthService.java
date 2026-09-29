package com.example.shopping.auth.service;
import com.example.shopping.auth.dto.request.ForgotPasswordRequestDto;
import com.example.shopping.auth.dto.response.LoginResponseDto;
public interface AuthService {
    void logout(String refreshToken);
    LoginResponseDto refreshToken(String refreshToken);
    void forgotPassword(ForgotPasswordRequestDto request);
}