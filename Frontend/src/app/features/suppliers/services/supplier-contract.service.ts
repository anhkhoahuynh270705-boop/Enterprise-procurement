import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { SupplierContract, SupplierContractRequest } from '../models/supplier-contract';

@Injectable({
  providedIn: 'root',
})
export class SupplierContractService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiBaseUrl}/supplier-contracts`;

  list() {
    return this.http.get<SupplierContract[]>(this.url);
  }

  create(contract: SupplierContractRequest, file: File | null) {
    return this.http.post<SupplierContract>(this.url, this.toFormData(contract, file));
  }

  update(id: string, contract: SupplierContractRequest, file: File | null) {
    return this.http.put<SupplierContract>(`${this.url}/${id}`, this.toFormData(contract, file));
  }

  delete(id: string) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  download(id: string) {
    return this.http.get(`${this.url}/${id}/download`, { responseType: 'blob' });
  }

  private toFormData(contract: SupplierContractRequest, file: File | null): FormData {
    const formData = new FormData();
    formData.append('contract', new Blob([JSON.stringify(contract)], { type: 'application/json' }));
    if (file) formData.append('file', file);
    return formData;
  }
}
