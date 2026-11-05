import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  CreatePurchaseOrderRequest,
  PurchaseOrder,
  PurchaseOrderApiResponse,
  PurchaseOrderPage,
  PurchaseOrderSearchParams,
  PurchaseOrderStatus,
  ReviewPurchaseOrderRequest,
} from '../models/purchase-order';

@Injectable({ providedIn: 'root' })
export class PurchaseOrderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/purchase-orders`;

  searchPurchaseOrders(
    filters: PurchaseOrderSearchParams,
  ): Observable<PurchaseOrderApiResponse<PurchaseOrderPage>> {
    let params = new HttpParams().set('page', filters.page).set('size', filters.size);
    const keys = [
      'keyword',
      'status',
      'supplierId',
      'ticket',
      'orderCode',
      'minimumAmount',
      'maximumAmount',
      'createdFrom',
      'createdTo',
      'deliveryFrom',
      'deliveryTo',
    ] as const;
    for (const key of keys) {
      const value = filters[key];
      if (value !== undefined && value !== '') params = params.set(key, value);
    }
    return this.http.get<PurchaseOrderApiResponse<PurchaseOrderPage>>(
      `${this.apiUrl}/page`,
      { params },
    );
  }

  getOrderedProcurementItemIds(ticketId: string): Observable<PurchaseOrderApiResponse<string[]>> {
    const params = new HttpParams().set('ticketId', ticketId);
    return this.http.get<PurchaseOrderApiResponse<string[]>>(
      `${this.apiUrl}/ordered-items`,
      { params },
    );
  }

  createPurchaseOrder(request: CreatePurchaseOrderRequest): Observable<PurchaseOrderApiResponse<PurchaseOrder>> {
    return this.http.post<PurchaseOrderApiResponse<PurchaseOrder>>(this.apiUrl, request);
  }

  updateStatus(id: string, status: PurchaseOrderStatus): Observable<PurchaseOrderApiResponse<PurchaseOrder>> {
    return this.http.patch<PurchaseOrderApiResponse<PurchaseOrder>>(
      `${this.apiUrl}/${id}/status`,
      { status },
    );
  }

  review(
    id: string,
    request: ReviewPurchaseOrderRequest,
  ): Observable<PurchaseOrderApiResponse<PurchaseOrder>> {
    return this.http.post<PurchaseOrderApiResponse<PurchaseOrder>>(
      `${this.apiUrl}/${id}/review`,
      request,
    );
  }
}
