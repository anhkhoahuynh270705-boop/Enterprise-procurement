package com.example.shopping.auth.service;

import com.example.shopping.auth.dto.request.ChangePasswordRequestDto;
import com.example.shopping.auth.dto.request.LoginRequestDto;
import com.example.shopping.auth.model.DirectLoginResult;

public interface DirectLoginService {

    DirectLoginResult login(LoginRequestDto req);

    void changePassword(ChangePasswordRequestDto req);
}
