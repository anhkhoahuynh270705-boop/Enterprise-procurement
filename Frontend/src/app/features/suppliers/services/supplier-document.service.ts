import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { SupplierDocument, SupplierOption } from '../models/supplier-document';


@Injectable({
  providedIn: 'root'
})
export class SupplierDocumentService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiBaseUrl + '/supplier-documents';

  list() {
    return this.http.get<SupplierDocument[]>(this.url);
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
