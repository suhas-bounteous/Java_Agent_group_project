// src/app/core/models/metrics.model.ts

export interface MetricsDTO {
  totalQueries: number;
  totalUpdates: number;
  totalErrors: number;
  totalLatencyNs: number;
  commits: number;
  rollbacks: number;
  activeConnections: number;
  slowQueries: number;
}

export interface MetricsEntity extends MetricsDTO {
  id: number;
  timestamp: number;
}

export interface MetricsSummaryDTO {
  totalQueries: number;
  totalErrors: number;
  errorRate: number;
  avgLatencyMs: number;
  activeConnections: number;
  slowQueries: number;
  commits: number;
  rollbacks: number;
}

export interface MetricsHistoryPoint {
  timestamp: number;
  avgLatencyMs: number;
  errorRate: number;
  activeConnections: number;
  totalQueries: number;
  slowQueries: number;
}
