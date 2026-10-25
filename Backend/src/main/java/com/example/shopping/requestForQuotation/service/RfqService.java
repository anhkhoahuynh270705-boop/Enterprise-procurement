package com.example.shopping.requestForQuotation.service;

import com.example.shopping.requestForQuotation.dto.request.CreateRfqRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqActionRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqDecisionRequest;
import com.example.shopping.requestForQuotation.dto.request.RfqSearchRequest;
import com.example.shopping.requestForQuotation.dto.request.RequestRfqRequoteRequest;
import com.example.shopping.requestForQuotation.dto.request.SelectRfqWinnersRequest;
import com.example.shopping.requestForQuotation.dto.request.UpdateRfqQuoteScoresRequest;
import com.example.shopping.requestForQuotation.dto.request.UpdateRfqRequest;
import com.example.shopping.requestForQuotation.dto.response.RfqDetailResponse;
import com.example.shopping.requestForQuotation.dto.response.RfqSummaryResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface RfqService {
    RfqDetailResponse create(CreateRfqRequest request, String username, boolean isAdmin);
    RfqDetailResponse update(UUID rfqId, UpdateRfqRequest request, String username, boolean isAdmin);
    Page<RfqSummaryResponse> search(RfqSearchRequest request);
    RfqDetailResponse getById(UUID rfqId, String username, boolean canViewAll);
    RfqDetailResponse submit(UUID rfqId, String username, boolean isAdmin);
    RfqDetailResponse decide(UUID rfqId, RfqDecisionRequest request, String checkerUsername);
    RfqDetailResponse cancel(UUID rfqId, RfqActionRequest request, String username, boolean isAdmin);
    RfqDetailResponse closeQuotes(UUID rfqId, RfqActionRequest request, String username, boolean isAdmin);
    RfqDetailResponse startComparison(UUID rfqId, String username, boolean isAdmin);
    RfqDetailResponse requestRequote(UUID rfqId, RequestRfqRequoteRequest request, String username);
    RfqDetailResponse updateQuoteScores(UUID rfqId, UUID quoteRoundId,
                                        UpdateRfqQuoteScoresRequest request, String username);
    RfqDetailResponse selectWinners(UUID rfqId, SelectRfqWinnersRequest request, String username);
}
