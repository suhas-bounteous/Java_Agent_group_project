// src/app/core/services/alert.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Alert, AlertSeverity, AlertType } from '../models/dashboard.model';
import { MetricsSummaryDTO } from '../models/metrics.model';

@Injectable({ providedIn: 'root' })
export class AlertService {

  private alerts$ = new BehaviorSubject<Alert[]>([]);

  // Configurable thresholds
  thresholds = {
    highLatencyMs: 100,
    highErrorRatePct: 5,
    excessConnections: 50,
    highRollbackRatePct: 10,
  };

  getAlerts() {
    return this.alerts$.asObservable();
  }

  getActiveAlerts() {
    return this.alerts$.getValue().filter(a => !a.acknowledged);
  }

  checkMetrics(summary: MetricsSummaryDTO): void {
    const newAlerts: Alert[] = [];

    if (summary.avgLatencyMs > this.thresholds.highLatencyMs) {
      newAlerts.push(this.createAlert(
        'HIGH_LATENCY', 'critical',
        `Avg query latency is ${summary.avgLatencyMs.toFixed(1)}ms (threshold: ${this.thresholds.highLatencyMs}ms)`,
        summary.avgLatencyMs, this.thresholds.highLatencyMs
      ));
    }

    if (summary.errorRate > this.thresholds.highErrorRatePct) {
      newAlerts.push(this.createAlert(
        'HIGH_ERROR_RATE', 'critical',
        `Error rate is ${summary.errorRate.toFixed(2)}% (threshold: ${this.thresholds.highErrorRatePct}%)`,
        summary.errorRate, this.thresholds.highErrorRatePct
      ));
    }

    if (summary.activeConnections > this.thresholds.excessConnections) {
      newAlerts.push(this.createAlert(
        'EXCESS_CONNECTIONS', 'warning',
        `${summary.activeConnections} active connections (threshold: ${this.thresholds.excessConnections})`,
        summary.activeConnections, this.thresholds.excessConnections
      ));
    }

    if (summary.slowQueries > 0) {
      newAlerts.push(this.createAlert(
        'SLOW_QUERY', 'warning',
        `${summary.slowQueries} slow queries detected in last interval`,
        summary.slowQueries, 0
      ));
    }

    const total = summary.commits + summary.rollbacks;
    if (total > 0) {
      const rollbackPct = (summary.rollbacks / total) * 100;
      if (rollbackPct > this.thresholds.highRollbackRatePct) {
        newAlerts.push(this.createAlert(
          'HIGH_ROLLBACK_RATE', 'warning',
          `Rollback rate is ${rollbackPct.toFixed(1)}% (threshold: ${this.thresholds.highRollbackRatePct}%)`,
          rollbackPct, this.thresholds.highRollbackRatePct
        ));
      }
    }

    if (newAlerts.length > 0) {
      const current = this.alerts$.getValue();
      // Keep last 100 alerts
      this.alerts$.next([...newAlerts, ...current].slice(0, 100));
    }
  }

  acknowledge(alertId: string): void {
    const alerts = this.alerts$.getValue().map(a =>
      a.id === alertId ? { ...a, acknowledged: true } : a
    );
    this.alerts$.next(alerts);
  }

  clearAll(): void {
    this.alerts$.next([]);
  }

  private createAlert(
    type: AlertType, severity: AlertSeverity,
    message: string, value: number, threshold: number
  ): Alert {
    return {
      id: Math.random().toString(36).slice(2),
      type, severity, message,
      timestamp: Date.now(),
      value, threshold,
      acknowledged: false,
    };
  }
}
