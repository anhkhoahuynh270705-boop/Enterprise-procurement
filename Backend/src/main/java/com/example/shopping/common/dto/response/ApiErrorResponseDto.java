package com.example.shopping.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ApiErrorResponseDto(String timestamp, int status, String error, String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) Object details) { }
