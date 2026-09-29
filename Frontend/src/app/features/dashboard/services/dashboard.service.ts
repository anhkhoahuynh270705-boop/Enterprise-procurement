import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { DashboardFilters, DashboardReport } from '../model/dashboard.model';

@Injectable({
  providedIn: 'root',
})
export class DashboardService {
  private readonly http = inject(HttpClient);
  getReport(filters: DashboardFilters, currency: string) {
    let params = new HttpParams().set('currency', currency);
    for (const [key, value] of Object.entries(filters)) {
      if (value) params = params.set(key, value);
    }
    return this.http.get<DashboardReport>(environment.apiBaseUrl + '/dashboard', { params });
  }
}
