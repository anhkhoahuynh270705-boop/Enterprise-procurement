import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { SupplierProposal, SupplierProposalRequest } from '../models/supplier-proposal';

@Injectable({
  providedIn: 'root'
})
export class SupplierProposalService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiBaseUrl + '/supplier-proposals';

  list() {
    return this.http.get<SupplierProposal[]>(this.url);
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
