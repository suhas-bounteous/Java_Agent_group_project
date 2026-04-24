// src/app/features/dashboard/dashboard.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { Subscription } from 'rxjs';

import { ApiService } from '../../core/services/api.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { AlertService } from '../../core/services/alert.service';
import { StatCardComponent } from '../../shared/components/stat-card/stat-card.component';
import { MetricsSummaryDTO, MetricsEntity } from '../../core/models/metrics.model';
import { EventDTO, EventStatsDTO } from '../../core/models/event.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, NgChartsModule, StatCardComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {

  summary: MetricsSummaryDTO | null = null;
  eventStats: EventStatsDTO | null = null;
  recentEvents: EventDTO[] = [];
  slowQueries: EventDTO[] = [];
  loading = true;
  backendError: string | null = null;
  hasData = false;

  private subs: Subscription[] = [];

  // ── Template-safe computed values (updated whenever summary changes) ──────
  avgLatencyDisplay = '—';
  errorRateDisplay = '—';
  totalOpsDisplay = '—';
  transactionsDisplay = '—';
  transactionSubtitle = '';
  commitPct = 0;
  rollbackPct = 0;
  rollbackRateDisplay = '0.0%';
  rollbackRateDanger = false;
  avgLatencyDanger = false;
  errorRateDanger = false;

  // ── Chart data ────────────────────────────────────────────────────────────
  latencyChartData: ChartData<'line'> = {
    labels: [],
    datasets: [{
      label: 'Avg Latency (ms)', data: [],
      borderColor: '#00d4ff', backgroundColor: 'rgba(0,212,255,0.08)',
      fill: true, tension: 0.4, pointRadius: 2, pointHoverRadius: 5, borderWidth: 2,
    }]
  };

  latencyChartOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false, animation: { duration: 300 },
    plugins: { legend: { display: false }, tooltip: { mode: 'index', intersect: false } },
    scales: {
      x: { ticks: { color: '#8892a4', font: { size: 10 }, maxTicksLimit: 8 }, grid: { color: 'rgba(255,255,255,0.04)' } },
      y: { ticks: { color: '#8892a4', font: { size: 10 } }, grid: { color: 'rgba(255,255,255,0.04)' }, beginAtZero: true }
    }
  };

  errorChartData: ChartData<'line'> = {
    labels: [],
    datasets: [{
      label: 'Error Rate (%)', data: [],
      borderColor: '#ff5252', backgroundColor: 'rgba(255,82,82,0.08)',
      fill: true, tension: 0.4, pointRadius: 2, borderWidth: 2,
    }]
  };

  errorChartOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false, animation: { duration: 300 },
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: '#8892a4', font: { size: 10 }, maxTicksLimit: 8 }, grid: { color: 'rgba(255,255,255,0.04)' } },
      y: { ticks: { color: '#8892a4', font: { size: 10 } }, grid: { color: 'rgba(255,255,255,0.04)' }, beginAtZero: true, max: 100 }
    }
  };

  operationsChartData: ChartData<'doughnut'> = {
    labels: ['SELECT/QUERY', 'UPDATE', 'COMMIT', 'ROLLBACK', 'Connection'],
    datasets: [{
      data: [0, 0, 0, 0, 0],
      backgroundColor: ['#00d4ff', '#7c4dff', '#00e676', '#ff5252', '#ffb300'],
      borderWidth: 0, hoverOffset: 6,
    }]
  };

  doughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true, maintainAspectRatio: false, cutout: '68%',
    plugins: { legend: { position: 'right', labels: { color: '#8892a4', padding: 12, font: { size: 11 }, boxWidth: 12 } } }
  };

  appChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [{
      label: 'DB Operations', data: [],
      backgroundColor: 'rgba(0,212,255,0.7)', borderColor: '#00d4ff', borderWidth: 1, borderRadius: 4,
    }]
  };

  barChartOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false, indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: {
      x: { ticks: { color: '#8892a4', font: { size: 10 } }, grid: { color: 'rgba(255,255,255,0.04)' }, beginAtZero: true },
      y: { ticks: { color: '#8892a4', font: { size: 11 } }, grid: { display: false } }
    }
  };

  connectionsChartData: ChartData<'line'> = {
    labels: [],
    datasets: [{
      label: 'Active Connections', data: [],
      borderColor: '#7c4dff', backgroundColor: 'rgba(124,77,255,0.1)',
      fill: true, tension: 0.4, pointRadius: 2, borderWidth: 2,
    }]
  };

  // ── Template-safe chart guards ────────────────────────────────────────────
  get hasLatencyData(): boolean {
    return (this.latencyChartData.labels?.length ?? 0) > 0;
  }
  get hasOpsData(): boolean {
    const d = this.operationsChartData.datasets[0].data as number[];
    return d.some(v => v > 0);
  }
  get hasAppData(): boolean {
    return (this.appChartData.labels?.length ?? 0) > 0;
  }
  get hasConnectionData(): boolean {
    return (this.connectionsChartData.labels?.length ?? 0) > 0;
  }
  get hasErrorData(): boolean {
    return (this.errorChartData.labels?.length ?? 0) > 0;
  }

  constructor(
    private api: ApiService,
    public dashSvc: DashboardService,
    private alertSvc: AlertService
  ) {}

  ngOnInit(): void {
    this.subs.push(
      this.dashSvc.getBackendError().subscribe(err => {
        this.backendError = err;
        if (err) this.loading = false;
      })
    );

    this.subs.push(
      this.dashSvc.createPollingObservable(() => this.api.getMetricsSummary())
        .subscribe(s => {
          this.summary = s;
          this.hasData = true;
          this.loading = false;
          this.updateSummaryDisplayValues(s);
          this.alertSvc.checkMetrics(s);
        })
    );

    this.subs.push(
      this.dashSvc.createPollingObservable(() => this.api.getMetricsHistory(30))
        .subscribe(h => { if (h && h.length > 0) this.updateTimeSeriesCharts(h); })
    );

    this.subs.push(
      this.dashSvc.createPollingObservable(() => this.api.getEventStats())
        .subscribe(stats => {
          if (stats) {
            this.eventStats = stats;
            this.updateOperationsChart(stats);
            this.updateAppChart(stats);
          }
        })
    );

    this.subs.push(
      this.dashSvc.createPollingObservable(() => this.api.getEvents(10))
        .subscribe(e => { if (e) this.recentEvents = e; })
    );

    this.subs.push(
      this.dashSvc.createPollingObservable(() => this.api.getSlowQueries(8))
        .subscribe(sq => { if (sq) this.slowQueries = sq; })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  // ── Pre-compute all template values here, never in the template ──────────

  private updateSummaryDisplayValues(s: MetricsSummaryDTO): void {
    this.totalOpsDisplay       = this.formatNumber(s.totalQueries);
    this.avgLatencyDisplay     = s.avgLatencyMs.toFixed(2) + ' ms';
    this.errorRateDisplay      = s.errorRate.toFixed(2) + '%';
    this.transactionsDisplay   = this.formatNumber((s.commits || 0) + (s.rollbacks || 0));
    this.transactionSubtitle   = `${s.commits} commits · ${s.rollbacks} rollbacks`;
    this.avgLatencyDanger      = s.avgLatencyMs > 100;
    this.errorRateDanger       = s.errorRate > 5;

    const total = (s.commits || 0) + (s.rollbacks || 0);
    if (total > 0) {
      this.commitPct         = Math.round((s.commits / total) * 100);
      this.rollbackPct       = Math.round((s.rollbacks / total) * 100);
      this.rollbackRateDisplay = ((s.rollbacks / total) * 100).toFixed(1) + '%';
      this.rollbackRateDanger  = (s.rollbacks / total) > 0.1;
    } else {
      this.commitPct         = 0;
      this.rollbackPct       = 0;
      this.rollbackRateDisplay = '0.0%';
      this.rollbackRateDanger  = false;
    }
  }

  private updateTimeSeriesCharts(history: MetricsEntity[]): void {
    const labels = history.map(h => {
      const d = new Date(h.timestamp);
      return d.getHours().toString().padStart(2,'0') + ':' +
             d.getMinutes().toString().padStart(2,'0') + ':' +
             d.getSeconds().toString().padStart(2,'0');
    });

    const latencies = history.map(h => {
      const ops = (h.totalQueries || 0) + (h.totalUpdates || 0);
      return ops > 0 ? Math.round((h.totalLatencyNs / 1_000_000) / ops * 100) / 100 : 0;
    });

    const errorRates = history.map(h => {
      const total = (h.totalQueries || 0) + (h.totalUpdates || 0);
      return total > 0 ? Math.round(((h.totalErrors || 0) / total) * 10000) / 100 : 0;
    });

    this.latencyChartData = { labels, datasets: [{ ...this.latencyChartData.datasets[0], data: latencies }] };
    this.errorChartData   = { labels, datasets: [{ ...this.errorChartData.datasets[0],   data: errorRates }] };
    this.connectionsChartData = {
      labels,
      datasets: [{ ...this.connectionsChartData.datasets[0], data: history.map(h => h.activeConnections || 0) }]
    };
  }

  private updateOperationsChart(stats: EventStatsDTO): void {
    const op = stats.byOperationType || {};
    this.operationsChartData = {
      ...this.operationsChartData,
      datasets: [{
        ...this.operationsChartData.datasets[0],
        data: [
          (op['QUERY'] || 0) + (op['UPDATE'] || 0) + (op['BATCH'] || 0),
          op['UPDATE']   || 0,
          op['COMMIT']   || 0,
          op['ROLLBACK'] || 0,
          (op['CONNECTION_OPEN'] || 0) + (op['CONNECTION_CLOSE'] || 0),
        ]
      }]
    };
  }

  private updateAppChart(stats: EventStatsDTO): void {
    if (!stats.byApplication || Object.keys(stats.byApplication).length === 0) return;
    const apps = Object.entries(stats.byApplication).sort((a, b) => b[1] - a[1]).slice(0, 6);
    this.appChartData = {
      labels: apps.map(([k]) => k),
      datasets: [{ ...this.appChartData.datasets[0], data: apps.map(([, v]) => v) }]
    };
  }

  // ── Public helpers used in template ──────────────────────────────────────
  formatDuration(ns: number): string { return this.dashSvc.formatDuration(ns); }
  formatNumber(n: number): string    { return this.dashSvc.formatNumber(n); }
  getOpIcon(op: string): string      { return this.dashSvc.getOperationIcon(op); }

  getOpClass(op: string): string {
    return 'op-' + (op || '').toLowerCase().replace(/_/g, '-');
  }
}