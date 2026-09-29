import { isPlatformBrowser } from '@angular/common';
import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../../features/auth/services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    if (!isPlatformBrowser(inject(PLATFORM_ID)))
        return next(req);
    const authService = inject(AuthService);
    if (!req.url.startsWith(authService.apiBaseUrl + '/') ||
        req.url.startsWith(authService.apiBaseUrl + '/auth/')) {
        return next(req);
    }
    const token = authService.getAccessToken();
    return next(token ? addToken(req, token) : req)
        .pipe(
            catchError((err: HttpErrorResponse) => {
                if (err.status !== 401)
                    return throwError(() => err
                    );
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
