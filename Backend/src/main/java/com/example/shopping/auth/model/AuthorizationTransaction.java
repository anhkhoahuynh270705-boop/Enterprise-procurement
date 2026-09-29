package com.example.shopping.auth.model;

import java.time.Instant;

public record AuthorizationTransaction(
    String state, 
    String browserToken, 
    String verifier, 
    String nonce, 
    Instant expiresAt
) { }
