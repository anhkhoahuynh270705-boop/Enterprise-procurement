import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { forkJoin, Observable, of, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiResponse, PageResult } from '../../procurement/model/procurement';
import {
  CreateRfqRequest,
  RfqActionRequest,
  RfqDecisionRequest,
  RfqDetailDto,
  RfqSearchParams,
  RfqStatus,
  RfqSummaryDto,
  SelectRfqWinnersRequest,
  UpdateRfqQuoteScoresRequest,
  UpdateRfqRequest,
} from '../model/rfq.models';

@Injectable({ providedIn: 'root' })
export class RfqApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/rfqs`;

  search(params: RfqSearchParams): Observable<ApiResponse<PageResult<RfqSummaryDto>>> {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size);
    for (const key of ['keyword', 'status', 'department', 'createdBy', 'createdFrom', 'createdTo'] as const) {
      const value = params[key];
      if (value !== undefined && value !== null && value !== '') httpParams = httpParams.set(key, value);
    }
    return this.http.get<ApiResponse<PageResult<RfqSummaryDto>>>(this.apiUrl, { params: httpParams });
  }

  exportExcel(
    params: Partial<RfqSearchParams>,
    statuses: RfqStatus[] = [],
  ): Observable<Blob> {
    let httpParams = new HttpParams();
    for (const key of ['keyword', 'status', 'department', 'createdBy', 'createdFrom', 'createdTo'] as const) {
      const value = params[key];
      if (value !== undefined && value !== null && value !== '') httpParams = httpParams.set(key, value);
    }
    for (const status of statuses) httpParams = httpParams.append('statuses', status);
    return this.http.get(`${this.apiUrl}/export`, { params: httpParams, responseType: 'blob' });
  }

  exportComparisonExcel(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/export/comparison`, { responseType: 'blob' });
  }

  exportComparisonPdf(id: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/export/comparison/pdf`, { responseType: 'blob' });
  }

  searchStatuses(
    params: RfqSearchParams,
    statuses: RfqStatus[],
  ): Observable<ApiResponse<PageResult<RfqSummaryDto>>[]> {
    return forkJoin(statuses.map((status) => this.search({ ...params, status, page: 0, size: 100 })));
  }

  getById(id: string): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.get<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}`);
  }

  create(request: CreateRfqRequest): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.post<ApiResponse<RfqDetailDto>>(this.apiUrl, request);
  }

  update(id: string, request: UpdateRfqRequest): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.put<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}`, request);
  }

  updateAndMaybeSubmit(
    id: string,
    request: UpdateRfqRequest,
    submitForApproval: boolean,
  ): Observable<ApiResponse<RfqDetailDto>> {
    return this.update(id, request).pipe(
      switchMap((response) => submitForApproval ? this.submit(response.data.id) : of(response)),
    );
  }

  submit(id: string): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.post<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}/submit`, {});
  }

  decide(id: string, request: RfqDecisionRequest): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.post<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}/decision`, request);
  }

  cancel(id: string, request: RfqActionRequest): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.post<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}/cancel`, request);
  }

  closeQuotes(id: string, request: RfqActionRequest): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.post<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}/close-quotes`, request);
  }

  startComparison(id: string): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.post<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}/comparison/start`, {});
  }

  requestRequote(id: string, supplierIds: string[], reason: string): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.post<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}/requote`, { supplierIds, reason });
  }

  updateScores(id: string, roundId: string, request: UpdateRfqQuoteScoresRequest): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.put<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}/quotes/${roundId}/scores`, request);
  }

  selectWinners(id: string, request: SelectRfqWinnersRequest): Observable<ApiResponse<RfqDetailDto>> {
    return this.http.post<ApiResponse<RfqDetailDto>>(`${this.apiUrl}/${id}/winners`, request);
  }

  sendInvitationLink(rfqId: string, supplierId: string): Observable<ApiResponse<{ rfqCode: string; supplierName: string; expiresAt: string }>> {
    return this.http.post<ApiResponse<{ rfqCode: string; supplierName: string; expiresAt: string }>>(
      `${this.apiUrl}/${rfqId}/invitations/${supplierId}/send-link`, {},
    );
  }

}
