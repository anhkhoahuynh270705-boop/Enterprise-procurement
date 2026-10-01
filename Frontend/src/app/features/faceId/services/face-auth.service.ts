import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../../auth/services/auth.service';
import { CurrentUser, LoginResponse } from '../../auth/model/auth';
import {
  FaceLoginRequest,
  FaceEnrollRequest,
  EnrolledStatusResponse,
  DetectResponse,
} from '../models/face.model';

@Injectable({ providedIn: 'root' })
export class FaceAuthService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly baseUrl = environment.apiBaseUrl + '/face';
  private readonly faceServiceUrl = 'http://localhost:8000';

  private readonly cookieOptions = {
    withCredentials: true,
    headers: { 'X-Requested-With': 'XMLHttpRequest' },
  };

  isEnrolled(username: string): Observable<EnrolledStatusResponse> {
    return this.http.get<EnrolledStatusResponse>(`${this.baseUrl}/enrolled`, {
      params: { username },
    });
  }

  /** Detect face quality directly from Python FaceService for real-time feedback */
  detectFace(imageB64: string): Observable<DetectResponse> {
    const raw = imageB64.replace(/^data:image\/\w+;base64,/, '');
    return this.http.post<DetectResponse>(`${this.faceServiceUrl}/face/detect`, {
      image: raw,
    });
  }

  loginWithFace(imageB64: string, username?: string): Observable<CurrentUser | null> {
    const payload: FaceLoginRequest = {
      image: imageB64,
      username: username?.trim() ? username.trim() : undefined,
    };
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/login`, payload, this.cookieOptions)
      .pipe(switchMap(() => this.auth.completeLogin()));
  }

  enrollFace(username: string, images: string[]): Observable<{ message: string }> {
    const rawImages = images.map((img) => img.replace(/^data:image\/\w+;base64,/, ''));
    const payload: FaceEnrollRequest = {
      username,
      images: rawImages,
    };
    return this.http.post<{ message: string }>(`${this.baseUrl}/enroll`, payload);
  }

  captureFrame(video: HTMLVideoElement, targetWidth = 360, targetHeight = 360): string | null {
    if (!video || video.videoWidth === 0 || video.videoHeight === 0) return null;
    const canvas = document.createElement('canvas');
    canvas.width = targetWidth;
    canvas.height = targetHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) return null;
    const vw = video.videoWidth;
    const vh = video.videoHeight;
    const minDim = Math.min(vw, vh);
    ctx.drawImage(
      video,
      (vw - minDim) / 2,
      (vh - minDim) / 2,
      minDim,
      minDim,
      0,
      0,
      targetWidth,
      targetHeight,
    );
    return canvas.toDataURL('image/jpeg', 0.82);
  }
}
