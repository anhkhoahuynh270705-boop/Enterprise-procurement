import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuditLogItem, AuditSearchParams, PagedResponse } from '../model/audit-log.model';
import { ApiResponse } from '../../procurement/model/procurement';

@Injectable({
  providedIn: 'root',
})
export class AuditLogService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/admin/audit-logs`;

  search(params: AuditSearchParams): Observable<PagedResponse<AuditLogItem>> {
    let httpParams = new HttpParams()
      .set('module', params.module ?? 'ALL')
      .set('status', params.status ?? 'ALL')
      .set('dateRange', params.dateRange ?? 'TODAY')
      .set('keyword', params.keyword ?? '')
      .set('page', String(params.page ?? 0))
      .set('size', String(params.size ?? 20));

    return this.http
      .get<ApiResponse<PagedResponse<AuditLogItem>>>(this.base, { params: httpParams })
      .pipe(map((r) => r.data));
  }
}
