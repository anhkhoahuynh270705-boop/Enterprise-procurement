package com.example.shopping.auth.model;

public record BrowserLoginStart(
    String url, 
    String browserToken
) { }
