import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../../features/auth/services/auth.service';
import { ToastService } from '../services/toast.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    if (!isPlatformBrowser(inject(PLATFORM_ID))) {
        return next(req);
    }

    const authService = inject(AuthService);
    const toast = inject(ToastService);

    if (!req.url.startsWith(authService.apiBaseUrl + '/')) {
        return next(req);
    }

    const isAuthEndpoint = req.url.startsWith(authService.apiBaseUrl + '/auth/');
    const token = authService.getAccessToken();

    // Không gắn Bearer token cho các endpoint auth (login, refresh...)
    const finalReq = (!isAuthEndpoint && token) ? addToken(req, token) : req;

    return next(finalReq).pipe(
        catchError((err: HttpErrorResponse) => {
            if (err.status === 429) {
                const seconds = err.error?.retryAfterSeconds || 60;
                toast.warning(`Bạn đang thao tác quá nhanh. Vui lòng thử lại sau ${seconds} giây!`);
                return throwError(() => err);
            }

            if (isAuthEndpoint || err.status !== 401) {
                return throwError(() => err);
            }

            return authService.refreshToken().pipe(
                catchError(refreshError => {
                    authService.logout();
                    return throwError(() => refreshError);
                }),
                switchMap(() => {
                    const refreshedToken = authService.getAccessToken();
                    return refreshedToken ? next(addToken(req, refreshedToken)) : throwError(() => err);
                })
            );
        })
    );
};

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
    return req.clone({
        setHeaders: {
            Authorization: 'Bearer ' + token
        }
    });
}
