import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateSupplierRequest,
  Supplier,
  SupplierImportApiResponse,
  SupplierSearchParams,
  UpdateSupplierRequest,
} from '../models/supplier';
import { ApiResponse, PageResult } from '../../procurement/model/procurement';

@Injectable({
  providedIn: 'root',
})
export class SupplierService {
  private readonly apiUrl = environment.apiBaseUrl + '/suppliers';
  private readonly http = inject(HttpClient);

  getSuppliers(keyword?: string): Observable<Supplier[]> {
    let params = new HttpParams();
    if (keyword && keyword.trim()) {
      params = params.set('keyword', keyword.trim());
    }
    return this.http.get<Supplier[]>(this.apiUrl, { params });
  }

  getSupplierById(id: string): Observable<Supplier> {
    return this.http.get<Supplier>(`${this.apiUrl}/${id}`);
  }

  createSupplier(data: CreateSupplierRequest): Observable<Supplier> {
    return this.http.post<Supplier>(this.apiUrl, data);
  }

  updateSupplier(id: string, data: UpdateSupplierRequest): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.apiUrl}/${id}`, data);
  }

  searchPage(filters: SupplierSearchParams): Observable<ApiResponse<PageResult<Supplier>>> {
    let params = new HttpParams().set('page', filters.page).set('size', filters.size);
    const keys = [
      'keyword',
      'status',
      'supplierId',
      'supplierCode',
      'contactPerson',
      'taxCode',
      'email',
      'phone',
      'city',
    ] as const;
    for (const key of keys) {
      const value = filters[key];
      if (value) {
        params = params.set(key, value);
      }
    }
    return this.http.get<ApiResponse<PageResult<Supplier>>>(`${this.apiUrl}/page`, { params });
  }

  deleteSupplier(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  exportAllSuppliers(format: 'EXCEL' | 'PDF' = 'EXCEL'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export-all?format=${format}`, {
      responseType: 'blob',
    });
  }

  exportSupplier(id: string, format: 'PDF' | 'EXCEL' = 'PDF'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/${id}?format=${format}`, {
      responseType: 'blob',
    });
  }

  downloadTemplate(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/template`, {
      responseType: 'blob',
    });
  }

  importSuppliers(file: File): Observable<SupplierImportApiResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<SupplierImportApiResponse>(`${this.apiUrl}/import`, formData);
  }
}
