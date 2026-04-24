// src/app/core/interceptors/error.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpRequest, HttpHandler, HttpEvent, HttpInterceptor, HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, catchError } from 'rxjs';

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Silently handle connection errors (backend may not be running)
        if (error.status === 0 || error.status === 503) {
          console.warn('[DB Monitor] Backend unavailable — using mock data');
          return throwError(() => error);
        }
        console.error('[DB Monitor] HTTP error:', error.status, error.message);
        return throwError(() => error);
      })
    );
  }
}
