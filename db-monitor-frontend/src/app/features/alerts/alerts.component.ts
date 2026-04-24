// src/app/features/alerts/alerts.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import { AlertService } from '../../core/services/alert.service';
import { Alert, AlertSeverity } from '../../core/models/dashboard.model';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alerts.component.html',
  styleUrls: ['./alerts.component.scss']
})
export class AlertsComponent implements OnInit, OnDestroy {

  allAlerts: Alert[] = [];
  filteredAlerts: Alert[] = [];
  severityFilter: AlertSeverity | '' = '';
  showAcknowledged = false;

  // Threshold settings
  thresholds = {
    highLatencyMs: 100,
    highErrorRatePct: 5,
    excessConnections: 50,
    highRollbackRatePct: 10
  };

  private subs: Subscription[] = [];

  constructor(public alertSvc: AlertService) {
    this.thresholds = { ...alertSvc.thresholds };
  }

  ngOnInit(): void {
    this.subs.push(
      this.alertSvc.getAlerts().subscribe(alerts => {
        this.allAlerts = alerts;
        this.applyFilter();
      })
    );
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  applyFilter(): void {
    this.filteredAlerts = this.allAlerts.filter(a => {
      if (!this.showAcknowledged && a.acknowledged) return false;
      if (this.severityFilter && a.severity !== this.severityFilter) return false;
      return true;
    });
  }

  acknowledge(id: string): void { this.alertSvc.acknowledge(id); }

  clearAll(): void { this.alertSvc.clearAll(); }

  saveThresholds(): void {
    Object.assign(this.alertSvc.thresholds, this.thresholds);
  }

  getAlertIcon(type: string): string {
    const icons: Record<string, string> = {
      HIGH_LATENCY: 'speed',
      HIGH_ERROR_RATE: 'error',
      EXCESS_CONNECTIONS: 'cable',
      SLOW_QUERY: 'hourglass_top',
      HIGH_ROLLBACK_RATE: 'undo',
      CONNECTION_SPIKE: 'trending_up',
    };
    return icons[type] || 'warning';
  }

  get criticalCount(): number { return this.allAlerts.filter(a => a.severity === 'critical' && !a.acknowledged).length; }
  get warningCount(): number { return this.allAlerts.filter(a => a.severity === 'warning' && !a.acknowledged).length; }
  get infoCount(): number { return this.allAlerts.filter(a => a.severity === 'info' && !a.acknowledged).length; }
}
