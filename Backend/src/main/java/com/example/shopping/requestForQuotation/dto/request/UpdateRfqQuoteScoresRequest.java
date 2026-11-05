package com.example.shopping.requestForQuotation.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateRfqQuoteScoresRequest(
        @NotEmpty
        List<@NotNull @Valid ScoreRequest> scores
) {}
