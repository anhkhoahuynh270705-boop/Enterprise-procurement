import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import {
  Observable,
  catchError,
  finalize,
  map,
  of,
  shareReplay,
  switchMap,
  tap,
  throwError,
} from 'rxjs';
import {
  AppRole,
  CurrentUser,
  LoginResponse,
  ForgotPasswordRequest,
  UserResponse,
} from '../model/auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly apiBaseUrl = environment.apiBaseUrl;
  private readonly apiUrl = this.apiBaseUrl + '/auth';
  // A non-simple header is required by the backend for cookie-authenticated requests.
  private readonly cookieOptions = {
    withCredentials: true,
    headers: { 'X-Requested-With': 'XMLHttpRequest' },
  };
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);
  private accessToken: string | null = null;
  private currentProfile: CurrentUser | null = null;
  private refreshRequest?: Observable<LoginResponse>;
  private sessionVersion = 0;
  private loggingOut = false;
  private sessionClosed = false;

  constructor() {
    this.clearLegacyStorage();
  }

  readonly loginUrl = this.apiUrl + '/authorize';

  completeLogin(): Observable<CurrentUser | null> {
    this.sessionVersion++;
    this.sessionClosed = false;
    this.currentProfile = null;
    this.accessToken = null;
    return this.refreshToken().pipe(
      switchMap(() => this.loadCurrentProfile()),
      tap((profile) => {
        if (!profile?.role) this.accessToken = null;
      }),
    );
  }
  logout(): void {
    if (this.loggingOut) return;
    this.loggingOut = true;
    this.sessionClosed = true;
    this.sessionVersion++;
    this.accessToken = null;
    this.currentProfile = null;
    this.clearLegacyStorage();
    if (isPlatformBrowser(this.platformId)) {
      // Wait for any rotated cookie before revoking it, so an in-flight refresh
      // cannot recreate the session after logout.
      const pending: Observable<LoginResponse | null> = this.refreshRequest ?? of(null);
      pending
        .pipe(
          catchError(() => of(null)),
          switchMap(() => this.http.post(this.apiUrl + '/logout', null, this.cookieOptions)),
          finalize(() => {
            this.loggingOut = false;
          }),
        )
        .subscribe({
          next: () => {
            void this.router.navigate(['/login']);
          },
          error: () => {
            console.warn('Logout failed; the server session could not be revoked.');
            void this.router.navigate(['/login']);
          },
        });
    } else {
      this.loggingOut = false;
      void this.router.navigate(['/login']);
    }
  }

  refreshToken(): Observable<LoginResponse> {
    if (this.sessionClosed) return throwError(() => new Error('Session closed'));
    if (!this.refreshRequest) {
      const version = this.sessionVersion;
      this.refreshRequest = this.http
        .post<LoginResponse>(this.apiUrl + '/refresh', null, this.cookieOptions)
        .pipe(
          tap((response) => {
            if (version === this.sessionVersion && !this.loggingOut) {
              this.accessToken = response.access_token;
            }
          }),
          finalize(() => {
            this.refreshRequest = undefined;
          }),
          shareReplay({
            bufferSize: 1,
            refCount: false,
          }),
        );
    }
    return this.refreshRequest;
  }

  restoreSession(): Observable<boolean> {
    if (this.sessionClosed || !isPlatformBrowser(this.platformId)) 
        return of(false);
    if (this.isLoggedIn()) 
        return of(true);
    return this.refreshToken().pipe(
      map(() => this.isLoggedIn()),
      catchError(() => {
        this.accessToken = null;
        return of(false);
      }),
    );
  }

  forgotPassword(data: ForgotPasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(this.apiUrl + '/forgot-password', data);
  }

  getAccessToken(): string | null {
    return this.accessToken;
  }

  isLoggedIn(): boolean {
    return !!this.accessToken;
  }

  hasRole(...roles: AppRole[]): boolean {
    const role = this.getCurrentUser()?.role;
    return !!role && roles.includes(role);
  }

  loadCurrentProfile(): Observable<CurrentUser | null> {
    const version = this.sessionVersion;
    return this.http.get<UserResponse | null>(this.apiBaseUrl + '/users/me').pipe(
      map((profile) =>
        profile
          ? {
              username: profile.username,
              email: profile.email,
              role: this.normalizeRole(profile.role),
            }
          : null,
      ),
      tap((profile) => {
        if (version === this.sessionVersion && !this.sessionClosed) this.currentProfile = profile;
      }),
      catchError(() => {
        this.currentProfile = null;
        return of(null);
      }),
    );
  }

  private normalizeRole(value: unknown): AppRole | undefined {
    if (typeof value !== 'string') return undefined;
    const role = value.replace(/^ROLE_/, '').replace(/^MAKER$/, 'USER');
    return ['ADMIN', 'USER', 'CHECKER'].includes(role) ? (role as AppRole) : undefined;
  }

  getCurrentUser(): CurrentUser | null {
    if (this.currentProfile) return this.currentProfile;
    const token = this.getAccessToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return {
        username: payload.preferred_username || payload.sub || 'User',
        email: payload.email,
        role: (['ADMIN', 'CHECKER', 'USER'] as AppRole[]).find((role) =>
          (Array.isArray(payload.realm_access?.roles) ? payload.realm_access.roles : []).some(
            (value: unknown) => this.normalizeRole(value) === role,
          ),
        ),
      };
    } catch {
      return null;
    }
  }

  private clearLegacyStorage(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    try {
      for (const key of ['access_token', 'refresh_token', 'current_user']) {
        localStorage.removeItem(key);
      }
    } catch {
      // Storage can be disabled; authentication no longer depends on it.
    }
  }
}
