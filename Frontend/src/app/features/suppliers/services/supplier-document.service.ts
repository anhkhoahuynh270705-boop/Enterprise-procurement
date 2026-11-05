import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { PageResult } from '../../procurement/model/procurement';
import { SupplierDocument, SupplierDocumentSearchParams, SupplierOption } from '../models/supplier-document';


@Injectable({
  providedIn: 'root'
})
export class SupplierDocumentService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiBaseUrl + '/supplier-documents';

  searchPage(params: SupplierDocumentSearchParams) {
    let httpParams = new HttpParams().set('page', params.page).set('size', params.size);
    for (const key of [
      'supplierId', 'status', 'keyword', 'submittedBy', 'reviewedBy', 'reviewComment',
      'submittedFrom', 'submittedTo', 'reviewedFrom', 'reviewedTo',
    ] as const) {
      const value = params[key];
      if (value !== undefined && value !== '') httpParams = httpParams.set(key, value);
    }
    return this.http.get<PageResult<SupplierDocument>>(`${this.url}/page`, { params: httpParams });
  }

  suppliers() {
    return this.http.get<SupplierOption[]>(this.url + '/suppliers');
  }

  upload(supplierId: string, file: File) {
    const body = new FormData();
    body.append('supplierId', supplierId);
    body.append('file', file);
    return this.http.post<SupplierDocument>(this.url, body);
  }

  download(id: string) {
    return this.http.get(`${this.url}/${id}/download`, { responseType: 'blob' });
  }

  review(id: string, approved: boolean, comment: string) {
    return this.http.post<SupplierDocument>(`${this.url}/${id}/review`, { approved, comment });
  }
}
