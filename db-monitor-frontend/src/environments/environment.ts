// src/environments/environment.ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8081',
  pollingIntervalMs: 5000,      // refresh dashboard every 5 seconds
  historyLimit: 60,              // fetch last 60 data points for charts
  slowQueryThresholdMs: 200,     // highlight queries slower than 200ms
};
