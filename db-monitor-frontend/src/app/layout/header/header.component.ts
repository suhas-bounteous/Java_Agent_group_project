// src/app/layout/header/header.component.ts
import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardService } from '../../core/services/dashboard.service';
import { AlertService } from '../../core/services/alert.service';
import { ConnectionStatus } from '../../core/models/dashboard.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit, OnDestroy {
  @Input() sidebarCollapsed = false;
  @Output() toggleSidebar = new EventEmitter<void>();

  connectionStatus!: ConnectionStatus;
  activeAlertCount = 0;
  currentTime = new Date();
  private subs: Subscription[] = [];
  private clockInterval: ReturnType<typeof setInterval> | null = null;

  constructor(
    private dashboardService: DashboardService,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    this.subs.push(
      this.dashboardService.getConnectionStatus().subscribe(s => this.connectionStatus = s),
      this.alertService.getAlerts().subscribe(alerts =>
        this.activeAlertCount = alerts.filter(a => !a.acknowledged).length
      )
    );
    this.clockInterval = setInterval(() => this.currentTime = new Date(), 1000);
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    if (this.clockInterval) clearInterval(this.clockInterval);
  }
}
