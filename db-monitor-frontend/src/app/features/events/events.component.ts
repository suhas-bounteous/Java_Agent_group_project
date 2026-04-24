// src/app/features/events/events.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import { ApiService } from '../../core/services/api.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { EventDTO } from '../../core/models/event.model';

@Component({
  selector: 'app-events',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './events.component.html',
  styleUrls: ['./events.component.scss']
})
export class EventsComponent implements OnInit, OnDestroy {

  events: EventDTO[] = [];
  filteredEvents: EventDTO[] = [];
  applicationNames: string[] = [];
  loading = true;
  paused = false;
  backendError: string | null = null;

  // Filters
  selectedApp = '';
  selectedOp = '';
  successFilter = '';
  searchQuery = '';

  operationTypes = ['QUERY', 'UPDATE', 'BATCH', 'COMMIT', 'ROLLBACK', 'CONNECTION_OPEN', 'CONNECTION_CLOSE'];

  private subs: Subscription[] = [];

  constructor(private api: ApiService, public dashSvc: DashboardService) {}

  ngOnInit(): void {
    // Track backend errors
    this.subs.push(
      this.dashSvc.getBackendError().subscribe(err => {
        this.backendError = err;
        if (err) this.loading = false;
      })
    );

    // Load application names for filter dropdown
    this.subs.push(
      this.api.getApplicationNames().subscribe({
        next: names => this.applicationNames = names || [],
        error: () => {} // silently ignore — filter just won't be populated
      })
    );

    // Poll live events
    this.subs.push(
      this.dashSvc.createPollingObservable(() =>
        this.api.getEvents(200, this.selectedApp || undefined, this.selectedOp || undefined)
      ).subscribe(events => {
        if (!this.paused) {
          this.events = events || [];
          this.applyFilters();
          this.loading = false;
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  applyFilters(): void {
    this.filteredEvents = this.events.filter(e => {
      if (this.selectedApp && e.applicationName !== this.selectedApp) return false;
      if (this.selectedOp && e.operationType !== this.selectedOp) return false;
      if (this.successFilter === 'success' && !e.success) return false;
      if (this.successFilter === 'error' && e.success) return false;
      if (this.searchQuery) {
        const q = this.searchQuery.toLowerCase();
        return (e.query || '').toLowerCase().includes(q) ||
               (e.applicationName || '').toLowerCase().includes(q) ||
               (e.operationType || '').toLowerCase().includes(q);
      }
      return true;
    });
  }

  togglePause(): void {
    this.paused = !this.paused;
  }

  clearFilters(): void {
    this.selectedApp = '';
    this.selectedOp = '';
    this.successFilter = '';
    this.searchQuery = '';
    this.applyFilters();
  }

  formatDuration(ns: number): string { return this.dashSvc.formatDuration(ns); }
  getOpIcon(op: string): string { return this.dashSvc.getOperationIcon(op); }
  trackByIndex(index: number): number { return index; }
}
