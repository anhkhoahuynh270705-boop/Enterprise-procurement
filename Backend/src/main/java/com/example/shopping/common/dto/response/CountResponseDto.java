package com.example.shopping.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CountResponseDto(
        long count) {
}
