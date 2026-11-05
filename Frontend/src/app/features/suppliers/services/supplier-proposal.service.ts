import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { PageResult } from '../../procurement/model/procurement';
import { SupplierProposal, SupplierProposalRequest, SupplierProposalSearchParams } from '../models/supplier-proposal';

@Injectable({
  providedIn: 'root',
})
export class SupplierProposalService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiBaseUrl + '/supplier-proposals';

  searchPage(filters: SupplierProposalSearchParams) {
    let params = new HttpParams().set('page', filters.page).set('size', filters.size);
    if (filters.keyword) params = params.set('keyword', filters.keyword);
    if (filters.status) params = params.set('status', filters.status);
    return this.http.get<PageResult<SupplierProposal>>(`${this.url}/page`, { params });
  }
  pendingCount() {
    return this.http.get<{ count: number }>(this.url + '/pending-count');
  }
  submit(request: SupplierProposalRequest) {
    return this.http.post<SupplierProposal>(this.url, request);
  }
  review(id: string, approved: boolean, comment: string) {
    return this.http.post<SupplierProposal>(`${this.url}/${id}/review`, { approved, comment });
  }
}
