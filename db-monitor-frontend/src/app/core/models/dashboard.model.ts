// src/app/core/models/dashboard.model.ts

export interface DashboardSummary {
  totalQueries: number;
  activeConnections: number;
  avgLatencyMs: number;
  errorRate: number;
  slowQueries: number;
  commits: number;
  rollbacks: number;
  monitoredApps: number;
}

export interface AppUsage {
  name: string;
  queryCount: number;
  errorCount: number;
  avgLatencyMs: number;
  activeConnections: number;
}

export interface Alert {
  id: string;
  type: AlertType;
  severity: AlertSeverity;
  message: string;
  timestamp: number;
  applicationName?: string;
  value?: number;
  threshold?: number;
  acknowledged: boolean;
}

export type AlertType =
  | 'HIGH_LATENCY'
  | 'HIGH_ERROR_RATE'
  | 'EXCESS_CONNECTIONS'
  | 'SLOW_QUERY'
  | 'HIGH_ROLLBACK_RATE'
  | 'CONNECTION_SPIKE';

export type AlertSeverity = 'info' | 'warning' | 'critical';

export interface ChartDataPoint {
  label: string;
  value: number;
}

export interface TimeSeriesPoint {
  time: string;
  value: number;
}

export interface ConnectionStatus {
  backendConnected: boolean;
  agentCount: number;
  lastUpdated: Date | null;
}
