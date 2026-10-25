package com.example.shopping.requestForQuotation.service;

import com.example.shopping.requestForQuotation.dto.request.supplierrequest.SupplierQuoteSubmissionRequest;
import com.example.shopping.requestForQuotation.dto.response.RfqQuoteRoundResponse;

public interface SupplierQuoteSubmissionService {
    RfqQuoteRoundResponse submit(SupplierQuoteSubmissionRequest request);
}
