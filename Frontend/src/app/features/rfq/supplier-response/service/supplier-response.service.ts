import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { ApiResponse } from '../../../procurement/model/procurement';
import {
  SupplierInvitationViewDto,
  SupplierQuoteSubmissionRequest,
} from '../model/supplier-response.models';

@Injectable({ providedIn: 'root' })
export class SupplierResponseService {
  private readonly http = inject(HttpClient);
  private readonly invitationUrl = `${environment.apiBaseUrl}/public/rfq-invitations`;

  resolveInvitation(token: string): Observable<ApiResponse<SupplierInvitationViewDto>> {
    return this.http.post<ApiResponse<SupplierInvitationViewDto>>(
      `${this.invitationUrl}/resolve`,
      { token },
    );
  }

  submitQuote(request: SupplierQuoteSubmissionRequest): Observable<ApiResponse<unknown>> {
    return this.http.post<ApiResponse<unknown>>(`${this.invitationUrl}/quotes`, request);
  }
}
