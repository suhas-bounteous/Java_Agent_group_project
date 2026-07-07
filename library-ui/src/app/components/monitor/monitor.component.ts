import { Component } from '@angular/core';
import { ApiService } from '../../services/api.service';

interface LogEntry {
  time: Date;
  type: 'info' | 'success' | 'error' | 'slow';
  message: string;
  duration?: number;
}

@Component({
  selector: 'app-monitor',
  templateUrl: './monitor.component.html',
  styleUrls: ['./monitor.component.scss']
})
export class MonitorComponent {
  logs: LogEntry[] = [];
  slowRunning = false;
  errorRunning = false;

  constructor(private api: ApiService) {}

  runSlowQuery() {
    this.slowRunning = true;
    const start = Date.now();
    this.addLog('info', 'Sending slow query to database (pg_sleep 5s)...');
    this.api.slowQuery().subscribe({
      next: data => {
        const duration = Date.now() - start;
        this.addLog('slow', `Slow query completed — returned ${data.length} books`, duration);
        this.slowRunning = false;
      },
      error: err => {
        const duration = Date.now() - start;
        this.addLog('error', `Slow query failed after ${duration}ms: ${err.message}`, duration);
        this.slowRunning = false;
      }
    });
  }

  runErrorQuery() {
    this.errorRunning = true;
    const start = Date.now();
    this.addLog('info', 'Sending error query to non-existent table...');
    this.api.errorQuery().subscribe({
      next: () => {
        this.addLog('success', 'Unexpected success (should have failed)');
        this.errorRunning = false;
      },
      error: err => {
        const duration = Date.now() - start;
        this.addLog('error', `DB Error triggered: HTTP ${err.status} — ${err.error?.error || 'Internal Server Error'}`, duration);
        this.errorRunning = false;
      }
    });
  }

  clearLogs() { this.logs = []; }

  private addLog(type: LogEntry['type'], message: string, duration?: number) {
    this.logs.unshift({ time: new Date(), type, message, duration });
  }
}
