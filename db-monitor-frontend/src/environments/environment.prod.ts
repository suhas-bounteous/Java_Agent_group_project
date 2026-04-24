// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiBaseUrl: '/api',          // Reverse proxy in production
  pollingIntervalMs: 5000,
  historyLimit: 60,
  slowQueryThresholdMs: 200,
};
