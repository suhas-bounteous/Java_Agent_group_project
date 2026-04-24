// src/app/core/models/event.model.ts

export interface EventDTO {
  applicationName: string;
  hostName: string;
  jvmId: string;
  databaseType: string;
  operationType: OperationType;
  timestamp: number;
  durationNs: number;
  success: boolean;
  metadata?: string;
  query?: string;
  slow?: boolean;
}

export type OperationType =
  | 'CONNECTION_OPEN'
  | 'CONNECTION_CLOSE'
  | 'QUERY'
  | 'UPDATE'
  | 'BATCH'
  | 'COMMIT'
  | 'ROLLBACK';

export interface EventStatsDTO {
  totalEvents: number;
  queryCount: number;
  connectionCount: number;
  transactionCount: number;
  errorCount: number;
  avgLatencyMs: number;
  byApplication: Record<string, number>;
  byOperationType: Record<string, number>;
}

export interface SlowQuery {
  query: string;
  durationMs: number;
  applicationName: string;
  timestamp: number;
  databaseType: string;
}
