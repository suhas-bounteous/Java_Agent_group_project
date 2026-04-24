// src/app/features/applications/applications.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgChartsModule } from 'ng2-charts';
import { ChartData, ChartConfiguration } from 'chart.js';
import { Subscription } from 'rxjs';

import { ApiService } from '../../core/services/api.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { EventStatsDTO } from '../../core/models/event.model';

interface AppRow {
  name: string;
  operations: number;
  pct: number;
}

@Component({
  selector: 'app-applications',
  standalone: true,
  imports: [CommonModule, NgChartsModule],
  templateUrl: './applications.component.html',
  styleUrls: ['./applications.component.scss']
})
export class ApplicationsComponent implements OnInit, OnDestroy {

  stats: EventStatsDTO | null = null;
  appRows: AppRow[] = [];
  loading = true;
  backendError: string | null = null;

  pieChartData: ChartData<'pie'> = {
    labels: [],
    datasets: [{ data: [], backgroundColor: [], borderWidth: 0, hoverOffset: 6 }]
  };

  pieOptions: ChartConfiguration['options'] = {
    responsive: true, maintainAspectRatio: false,
    plugins: { legend: { position: 'right', labels: { color: '#8892a4', font: { size: 11 }, padding: 14, boxWidth: 12 } } }
  };

  private subs: Subscription[] = [];
  private colors = ['#00d4ff', '#7c4dff', '#00e676', '#ffb300', '#ff5252', '#e040fb'];

  constructor(private api: ApiService, public dashSvc: DashboardService) {}

  ngOnInit(): void {
    this.subs.push(
      this.dashSvc.getBackendError().subscribe(err => {
        this.backendError = err;
        if (err) this.loading = false;
      })
    );

    this.subs.push(
      this.dashSvc.createPollingObservable(() => this.api.getEventStats())
        .subscribe(stats => {
          if (stats) {
            this.stats = stats;
            this.buildAppRows(stats);
            this.buildPieChart(stats);
          }
          this.loading = false;
        })
    );
  }

  ngOnDestroy(): void { this.subs.forEach(s => s.unsubscribe()); }

  private buildAppRows(stats: EventStatsDTO): void {
    const total = stats.totalEvents || 1;
    this.appRows = Object.entries(stats.byApplication || {})
      .sort((a, b) => b[1] - a[1])
      .map(([name, operations]) => ({
        name,
        operations,
        pct: Math.round((operations / total) * 100)
      }));
  }

  private buildPieChart(stats: EventStatsDTO): void {
    const entries = Object.entries(stats.byApplication || {}).sort((a, b) => b[1] - a[1]);
    if (entries.length === 0) return;

    this.pieChartData = {
      labels: entries.map(([k]) => k),
      datasets: [{
        data: entries.map(([, v]) => v),
        backgroundColor: entries.map((_, i) => this.colors[i % this.colors.length]),
        borderWidth: 0,
        hoverOffset: 6,
      }]
    };
  }

  formatNumber(n: number): string { return this.dashSvc.formatNumber(n); }
  getColorForIndex(i: number): string { return this.colors[i % this.colors.length]; }

  get hasAppData(): boolean {
    return this.appRows.length > 0;
  }

  get hasOpData(): boolean {
    return this.stats !== null && Object.keys(this.stats.byOperationType || {}).length > 0;
  }
}
