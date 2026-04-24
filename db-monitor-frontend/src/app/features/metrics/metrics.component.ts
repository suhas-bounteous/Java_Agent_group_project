// src/app/features/metrics/metrics.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { Subscription } from 'rxjs';

import { ApiService } from '../../core/services/api.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { MetricsEntity, MetricsSummaryDTO } from '../../core/models/metrics.model';

@Component({
  selector: 'app-metrics',
  standalone: true,
  imports: [CommonModule, NgChartsModule],
  templateUrl: './metrics.component.html',
  styleUrls: ['./metrics.component.scss']
})
export class MetricsComponent implements OnInit, OnDestroy {

  metricsHistory: MetricsEntity[] = [];
  // latest now comes from /metrics/summary (cumulative totals), not from a single snapshot row
  summary: MetricsSummaryDTO | null = null;
  loading = true;
  backendError: string | null = null;

  private subs: Subscription[] = [];

  // ── Stacked bar: queries + updates ────────────────────────────────────────
  throughputChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      { label: 'Queries', data: [], backgroundColor: 'rgba(0,212,255,0.7)', borderColor: '#00d4ff', borderWidth: 1, borderRadius: 3, stack: 'throughput' },
      { label: 'Updates', data: [], backgroundColor: 'rgba(124,77,255,0.7)', borderColor: '#7c4dff', borderWidth: 1, borderRadius: 3, stack: 'throughput' },
    ]
  };

  stackedBarOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false, animation: { duration: 300 },
    plugins: { legend: { labels: { color: '#8892a4', font: { size: 11 }, boxWidth: 12 } } },
    scales: {
      x: { stacked: true, ticks: { color: '#8892a4', font: { size: 10 }, maxTicksLimit: 10 }, grid: { color: 'rgba(255,255,255,0.04)' } },
      y: { stacked: true, ticks: { color: '#8892a4', font: { size: 10 } }, grid: { color: 'rgba(255,255,255,0.04)' }, beginAtZero: true }
    }
  };

  // ── Dual-axis: errors + slow queries ─────────────────────────────────────
  errorSlowChartData: ChartData<'line'> = {
    labels: [],
    datasets: [
      { label: 'Errors', data: [], borderColor: '#ff5252', backgroundColor: 'rgba(255,82,82,0.07)', fill: true, tension: 0.4, pointRadius: 2, borderWidth: 2, yAxisID: 'y' },
      { label: 'Slow Queries', data: [], borderColor: '#ffb300', backgroundColor: 'rgba(255,179,0,0.07)', fill: true, tension: 0.4, pointRadius: 2, borderWidth: 2, yAxisID: 'y1' },
    ]
  };

  dualAxisOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false, animation: { duration: 300 },
    plugins: { legend: { labels: { color: '#8892a4', font: { size: 11 }, boxWidth: 12 } } },
    scales: {
      x:  { ticks: { color: '#8892a4', font: { size: 10 }, maxTicksLimit: 10 }, grid: { color: 'rgba(255,255,255,0.04)' } },
      y:  { position: 'left',  ticks: { color: '#ff5252', font: { size: 10 } }, grid: { color: 'rgba(255,255,255,0.04)' }, beginAtZero: true },
      y1: { position: 'right', ticks: { color: '#ffb300', font: { size: 10 } }, grid: { display: false }, beginAtZero: true }
    }
  };

  // ── Connections ────────────────────────────────────────────────────────────
  connectionsChartData: ChartData<'line'> = {
    labels: [],
    datasets: [{ label: 'Active Connections', data: [], borderColor: '#00e676', backgroundColor: 'rgba(0,230,118,0.08)', fill: true, tension: 0.4, pointRadius: 2, borderWidth: 2 }]
  };

  lineOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false, animation: { duration: 300 },
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: '#8892a4', font: { size: 10 }, maxTicksLimit: 10 }, grid: { color: 'rgba(255,255,255,0.04)' } },
      y: { ticks: { color: '#8892a4', font: { size: 10 } }, grid: { color: 'rgba(255,255,255,0.04)' }, beginAtZero: true }
    }
  };

  // ── Latency ────────────────────────────────────────────────────────────────
  latencyChartData: ChartData<'line'> = {
    labels: [],
    datasets: [{ label: 'Avg Latency (ms)', data: [], borderColor: '#00d4ff', backgroundColor: 'rgba(0,212,255,0.07)', fill: true, tension: 0.4, pointRadius: 2, borderWidth: 2 }]
  };

  constructor(private api: ApiService, public dashSvc: DashboardService) {}

  ngOnInit(): void {
    this.subs.push(
      this.dashSvc.getBackendError().subscribe(err => {
        this.backendError = err;
        if (err) this.loading = false;
      })
    );

    // FIX: Poll /metrics/summary for cumulative KPI totals
    this.subs.push(
      this.dashSvc.createPollingObservable(() => this.api.getMetricsSummary())
        .subscribe(s => {
          if (s) this.summary = s;
          this.loading = false;
        })
    );

    // Poll /metrics/history for time-series charts only
    this.subs.push(
      this.dashSvc.createPollingObservable(() => this.api.getMetricsHistory(60))
        .subscribe(history => {
          if (history && history.length > 0) {
            this.metricsHistory = history;
            this.updateCharts(history);
          }
          this.loading = false;
        })
    );
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  get recentMetricsRows(): MetricsEntity[] {
    return this.metricsHistory.slice().reverse().slice(0, 20);
  }

  private updateCharts(history: MetricsEntity[]): void {
    const labels = history.map(h => {
      const d = new Date(h.timestamp);
      return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}:${d.getSeconds().toString().padStart(2,'0')}`;
    });

    this.throughputChartData = {
      labels,
      datasets: [
        { ...this.throughputChartData.datasets[0], data: history.map(h => h.totalQueries || 0) },
        { ...this.throughputChartData.datasets[1], data: history.map(h => h.totalUpdates || 0) },
      ]
    };

    this.errorSlowChartData = {
      labels,
      datasets: [
        { ...this.errorSlowChartData.datasets[0], data: history.map(h => h.totalErrors || 0) },
        { ...this.errorSlowChartData.datasets[1], data: history.map(h => h.slowQueries || 0) },
      ]
    };

    this.connectionsChartData = {
      labels,
      datasets: [{ ...this.connectionsChartData.datasets[0], data: history.map(h => h.activeConnections || 0) }]
    };

    this.latencyChartData = {
      labels,
      datasets: [{
        ...this.latencyChartData.datasets[0],
        data: history.map(h => {
          const total = (h.totalQueries || 0) + (h.totalUpdates || 0);
          return total > 0 ? Math.round(((h.totalLatencyNs || 0) / 1_000_000 / total) * 100) / 100 : 0;
        })
      }]
    };
  }

  formatNumber(n: number): string { return this.dashSvc.formatNumber(n); }

  calcAvgLatency(m: MetricsEntity): string {
    const total = (m.totalQueries || 0) + (m.totalUpdates || 0);
    if (total === 0) return '0.00 ms';
    return ((m.totalLatencyNs || 0) / 1_000_000 / total).toFixed(2) + ' ms';
  }
}