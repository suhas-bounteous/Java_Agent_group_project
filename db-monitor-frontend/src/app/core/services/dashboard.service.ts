// src/app/core/services/dashboard.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, interval, switchMap, startWith, catchError, EMPTY } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ConnectionStatus } from '../models/dashboard.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DashboardService {

  private connectionStatus$ = new BehaviorSubject<ConnectionStatus>({
    backendConnected: false,
    agentCount: 0,
    lastUpdated: null,
  });

  private backendError$ = new BehaviorSubject<string | null>(null);

  getConnectionStatus(): Observable<ConnectionStatus> {
    return this.connectionStatus$.asObservable();
  }

  getBackendError(): Observable<string | null> {
    return this.backendError$.asObservable();
  }

  markConnected(): void {
    this.backendError$.next(null);
    const curr = this.connectionStatus$.getValue();
    this.connectionStatus$.next({ ...curr, backendConnected: true, lastUpdated: new Date() });
  }

  markDisconnected(msg: string): void {
    this.backendError$.next(msg);
    const curr = this.connectionStatus$.getValue();
    this.connectionStatus$.next({ ...curr, backendConnected: false, lastUpdated: new Date() });
  }

  /**
   * Polls the backend at the configured interval.
   * On HTTP error: marks backend as disconnected and does NOT emit — component
   * keeps its last-good data instead of receiving fake/empty values.
   */
  createPollingObservable<T>(request: () => Observable<T>): Observable<T> {
    return interval(environment.pollingIntervalMs).pipe(
      startWith(0),
      switchMap(() =>
        request().pipe(
          tap(() => this.markConnected()),
          catchError(err => {
            const msg = err.status === 0
              ? 'Cannot reach backend — is it running on port 8081?'
              : `Backend error ${err.status}: ${err.statusText}`;
            this.markDisconnected(msg);
            return EMPTY; // swallow error, keep polling, don't update UI with bad data
          })
        )
      )
    );
  }

  // ── Formatting helpers ─────────────────────────────────────────────────────

  nsToMs(ns: number): number {
    return Math.round((ns / 1_000_000) * 100) / 100;
  }

  formatNumber(n: number): string {
    if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M';
    if (n >= 1_000) return (n / 1_000).toFixed(1) + 'K';
    return n.toString();
  }

  formatDuration(ns: number): string {
    const ms = ns / 1_000_000;
    if (ms < 1) return `${(ns / 1000).toFixed(0)}µs`;
    if (ms < 1000) return `${ms.toFixed(1)}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  }

  getOperationIcon(op: string): string {
    const icons: Record<string, string> = {
      QUERY: 'search',
      UPDATE: 'edit',
      BATCH: 'playlist_add',
      COMMIT: 'check_circle',
      ROLLBACK: 'undo',
      CONNECTION_OPEN: 'link',
      CONNECTION_CLOSE: 'link_off',
    };
    return icons[op] || 'storage';
  }
}

