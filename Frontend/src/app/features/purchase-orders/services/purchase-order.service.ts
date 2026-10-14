import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  CreatePurchaseOrderRequest,
  PurchaseOrder,
  PurchaseOrderApiResponse,
  PurchaseOrderStatus,
  ReviewPurchaseOrderRequest,
} from '../models/purchase-order';

@Injectable({ providedIn: 'root' })
export class PurchaseOrderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/purchase-orders`;

  getPurchaseOrders(status?: PurchaseOrderStatus): Observable<PurchaseOrderApiResponse<PurchaseOrder[]>> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<PurchaseOrderApiResponse<PurchaseOrder[]>>(this.apiUrl, { params });
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
