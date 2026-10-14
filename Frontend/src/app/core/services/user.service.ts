import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UpdateMyProfileRequest, UserResponse } from '../../features/auth/model/auth';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  getMyProfile(): Observable<UserResponse | null> {
    return this.http.get<UserResponse | null>(`${this.baseUrl}/users/me`);
  }

  updateMyProfile(body: UpdateMyProfileRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/users/me`, body);
  }

  uploadMyAvatar(file: File): Observable<UserResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<UserResponse>(`${this.baseUrl}/users/me/avatar`, formData);
  }

  resolveAvatarUrl(avatarUrl: string | null | undefined): string | null {
    const raw = avatarUrl?.trim();
    if (!raw) return null;
    if (raw.startsWith('/api/')) {
      return `${this.baseUrl.replace(/\/api\/?$/, '')}${raw}`;
    }
    if (raw.startsWith('http://') || raw.startsWith('https://') || raw.startsWith('data:image/')) {
      return raw;
    }
    return null;
  }
}
