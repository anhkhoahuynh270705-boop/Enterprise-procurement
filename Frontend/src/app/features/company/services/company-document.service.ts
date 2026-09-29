import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { CompanyDocument } from '../model/company.model'

@Injectable({ 
  providedIn: 'root' 
})

export class CompanyDocumentService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiBaseUrl + '/company-documents';

  list() { 
    return this.http.get<CompanyDocument[]>(this.url); 
  }
  upload(title: string, category: string, description: string, file: File) {
    const body = new FormData();
    body.append('title', title); 
    body.append('category', category);
    body.append('description', description); 
    body.append('file', file);
    return this.http.post<CompanyDocument>(this.url, body);
  }
  download(id: string) { 
    return this.http.get(`${this.url}/${id}/download`, { 
      responseType: 'blob' 
    }); 
  }
  delete(id: string) { 
    return this.http.delete<void>(`${this.url}/${id}`); 
  }
}
