// src/app/core/services/api.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EventDTO, EventStatsDTO } from '../models/event.model';
import { MetricsEntity, MetricsSummaryDTO } from '../models/metrics.model';

@Injectable({ providedIn: 'root' })
export class ApiService {

  private base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  // ── Metrics ────────────────────────────────────────────────────────────────

  getLatestMetrics(): Observable<MetricsEntity> {
    return this.http.get<MetricsEntity>(`${this.base}/metrics/latest`);
  }

  getMetricsHistory(limit = 50): Observable<MetricsEntity[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<MetricsEntity[]>(`${this.base}/metrics/history`, { params });
  }

  getMetricsSummary(): Observable<MetricsSummaryDTO> {
    return this.http.get<MetricsSummaryDTO>(`${this.base}/metrics/summary`);
  }

  // ── Events ─────────────────────────────────────────────────────────────────

  getEvents(limit = 100, appName?: string, operationType?: string): Observable<EventDTO[]> {
    let params = new HttpParams().set('limit', limit);
    if (appName) params = params.set('appName', appName);
    if (operationType) params = params.set('operationType', operationType);
    return this.http.get<EventDTO[]>(`${this.base}/events`, { params });
  }

  getSlowQueries(limit = 20): Observable<EventDTO[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<EventDTO[]>(`${this.base}/events/slow-queries`, { params });
  }

  getApplicationNames(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/events/applications`);
  }

  getEventStats(): Observable<EventStatsDTO> {
    return this.http.get<EventStatsDTO>(`${this.base}/events/stats`);
  }
}
