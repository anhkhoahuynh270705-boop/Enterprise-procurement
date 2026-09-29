package com.example.shopping.auth.service;

import com.example.shopping.auth.model.AuthorizationTransaction;

public interface AuthorizationTransactions {
    AuthorizationTransaction create();
    AuthorizationTransaction consume(String state, String browserToken);
}
