package com.example.shopping.auth.model;

import com.example.shopping.auth.dto.response.LoginResponseDto;

public sealed interface DirectLoginResult permits DirectLoginResult.Success,
        DirectLoginResult.RequireUpdatePassword, DirectLoginResult.RequireVerifyEmail {
    record Success(LoginResponseDto tokens) implements DirectLoginResult { }
    record RequireUpdatePassword(String username) implements DirectLoginResult { }
    record RequireVerifyEmail() implements DirectLoginResult { }

    static DirectLoginResult success(LoginResponseDto tokens) { 
        return new Success(tokens); 
    }
    static DirectLoginResult requireUpdatePassword(String username) { 
        return new RequireUpdatePassword(username); 
    }
    static DirectLoginResult requireVerifyEmail() { 
        return new RequireVerifyEmail(); 
    }
}
