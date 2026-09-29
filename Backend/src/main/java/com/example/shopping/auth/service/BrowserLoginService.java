package com.example.shopping.auth.service;

import com.example.shopping.auth.dto.response.LoginResponseDto;
import com.example.shopping.auth.model.BrowserLoginStart;

public interface BrowserLoginService {

    BrowserLoginStart begin();

    LoginResponseDto complete(
            String state,
            String browserToken,
            String code,
            String error
    );
}
