import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ApiResponse,
  CamundaTask,
  CheckerReviewRequest,
  CreateProcurementTicketRequest,
  ImportResult,
  PageResult,
  ProcurementTicket
} from '../model/procurement';

@Injectable({
  providedIn: 'root'
})
export class ProcurementService {
  private readonly apiUrl = environment.apiBaseUrl + '/procurement';
  private http = inject(HttpClient);

  getAllTickets(status?: string, maker?: string): Observable<ApiResponse<ProcurementTicket[]>> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (maker) params = params.set('maker', maker);
    return this.http.get<ApiResponse<ProcurementTicket[]>>(`${this.apiUrl}/tickets`, { params });
  }

  getTicketPage(status: string | undefined, page: number, size: number): Observable<ApiResponse<PageResult<ProcurementTicket>>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PageResult<ProcurementTicket>>>(`${this.apiUrl}/tickets/page`, { params });
  }

  getTicketById(id: string): Observable<ApiResponse<ProcurementTicket>> {
    return this.http.get<ApiResponse<ProcurementTicket>>(`${this.apiUrl}/tickets/${id}`);
  }

  createTicket(data: CreateProcurementTicketRequest): Observable<ApiResponse<ProcurementTicket>> {
    return this.http.post<ApiResponse<ProcurementTicket>>(`${this.apiUrl}/tickets`, data);
  }

  updateTicket(id: string, data: any): Observable<ApiResponse<ProcurementTicket>> {
    return this.http.put<ApiResponse<ProcurementTicket>>(`${this.apiUrl}/tickets/${id}`, data);
  }

  submitTicket(id: string): Observable<ApiResponse<ProcurementTicket>> {
    return this.http.post<ApiResponse<ProcurementTicket>>(`${this.apiUrl}/tickets/${id}/submit`, {});
  }

  deleteTicket(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/tickets/${id}`);
  }

  getPendingCheckerTasks(): Observable<ApiResponse<CamundaTask[]>> {
    return this.http.get<ApiResponse<CamundaTask[]>>(`${this.apiUrl}/tasks`);
  }

  reviewTask(taskId: string, review: CheckerReviewRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.apiUrl}/tasks/${taskId}/review`, review);
  }

  exportTicket(id: string, format: 'PDF' | 'EXCEL' = 'PDF'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export/${id}?format=${format}`, {
      responseType: 'blob'
    });
  }

  exportTicketDetail(id: string, format: 'PDF' | 'EXCEL' = 'PDF'): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export-detail/${id}?format=${format}`, {
      responseType: 'blob'
    });
  }

  exportAllTickets(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/export-all?format=EXCEL`, {
      responseType: 'blob'
    });
  }

  // downloadTemplate(): Observable<Blob> {
  //   return this.http.get(`${this.apiUrl}/template`, {
  //     responseType: 'blob'
  //   });
  // }

  importExcel(file: File): Observable<ApiResponse<ImportResult>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<ImportResult>>(`${this.apiUrl}/import`, formData);
  }
}
