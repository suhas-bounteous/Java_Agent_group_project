# DB Monitor Frontend — Quick Start

## Prerequisites
- Node.js 18+
- Angular CLI 16: `npm install -g @angular/cli@16`
- Your Spring Boot backend running on port 8081

---

## Installation

```bash
# 1. Navigate into the project
cd db-monitor-frontend

# 2. Install dependencies
npm install

# 3. Start development server
npm start
# → Opens at http://localhost:4200
```

---

## Project File Checklist

Ensure all these files exist before running `npm start`:

```
src/
├── index.html                          ✅
├── main.ts                             ✅
├── styles.scss                         ✅
├── environments/
│   ├── environment.ts                  ✅
│   └── environment.prod.ts             ✅
└── app/
    ├── app.component.ts                ✅
    ├── app.component.html              ✅
    ├── app.component.scss              ✅
    ├── app.module.ts                   ✅
    ├── app-routing.module.ts           ✅
    ├── core/
    │   ├── models/
    │   │   ├── event.model.ts          ✅
    │   │   ├── metrics.model.ts        ✅
    │   │   └── dashboard.model.ts      ✅
    │   ├── services/
    │   │   ├── api.service.ts          ✅
    │   │   ├── dashboard.service.ts    ✅
    │   │   └── alert.service.ts        ✅
    │   └── interceptors/
    │       └── error.interceptor.ts    ✅
    ├── shared/
    │   └── components/
    │       └── stat-card/
    │           ├── stat-card.component.ts    ✅
    │           ├── stat-card.component.html  ✅
    │           └── stat-card.component.scss  ✅
    ├── layout/
    │   ├── header/
    │   │   ├── header.component.ts     ✅
    │   │   ├── header.component.html   ✅
    │   │   └── header.component.scss   ✅
    │   └── sidebar/
    │       ├── sidebar.component.ts    ✅
    │       ├── sidebar.component.html  ✅
    │       └── sidebar.component.scss  ✅
    └── features/
        ├── dashboard/
        │   ├── dashboard.component.ts   ✅
        │   ├── dashboard.component.html ✅
        │   └── dashboard.component.scss ✅
        ├── events/
        │   ├── events.component.ts      ✅
        │   ├── events.component.html    ✅
        │   └── events.component.scss    ✅
        ├── metrics/
        │   ├── metrics.component.ts     ✅
        │   ├── metrics.component.html   ✅
        │   └── metrics.component.scss   ✅
        ├── applications/
        │   ├── applications.component.ts   ✅
        │   ├── applications.component.html ✅
        │   └── applications.component.scss ✅
        └── alerts/
            ├── alerts.component.ts      ✅
            ├── alerts.component.html    ✅
            └── alerts.component.scss    ✅
```

---

## Dashboard Pages

| Route | Description |
|-------|-------------|
| `/dashboard` | KPI cards + 6 live charts + events table + slow queries |
| `/events` | Full event stream with filters (app, operation, status, search) |
| `/metrics` | Metrics history with stacked bar, dual-axis line charts + raw log table |
| `/applications` | Per-service breakdown with pie chart + progress bars |
| `/alerts` | Active alerts list + configurable thresholds |

---

## Adding Backend GET Endpoints

The dashboard works with mock data out of the box. To connect live data, add these endpoints to your Spring Boot backend (see `BACKEND_ADDITIONS.md`):

| Endpoint | Description |
|----------|-------------|
| `GET /metrics/latest` | Returns most recent `DbMetricsEntity` |
| `GET /metrics/history?limit=60` | Returns last N metrics snapshots |
| `GET /metrics/summary` | Returns computed summary (error rate, avg latency etc.) |
| `GET /events?limit=200&appName=&operationType=` | Paginated event list with filters |
| `GET /events/slow-queries?limit=20` | Slow queries only |
| `GET /events/applications` | Distinct application names |
| `GET /events/stats` | Aggregated stats (counts by app, operation type) |

---

## Design System

| Variable | Value | Usage |
|----------|-------|-------|
| `--bg-primary` | `#080c18` | Page background |
| `--surface-1` | `#0d1224` | Header/sidebar |
| `--card-bg` | `#111827` | Card backgrounds |
| `--accent-color` | `#00d4ff` | Primary accent (cyan) |
| `--color-success` | `#00e676` | OK states, commits |
| `--color-warning` | `#ffb300` | Slow queries, warnings |
| `--color-danger` | `#ff5252` | Errors, rollbacks |
| `--color-purple` | `#9c6dff` | Secondary accent |

**Fonts:**
- UI text: `Inter`
- Numbers/code: `JetBrains Mono`
- Icons: `Material Icons`

---

## Live Refresh Behaviour

All dashboard data auto-polls every **5 seconds** (configurable in `environment.ts`).  
The sidebar shows a green pulsing dot when the backend is reachable.  
If the backend is offline, all views fall back to realistic mock data silently.

---

## Build for Production

```bash
ng build --configuration production
# Output: dist/db-monitor-frontend/
```

Serve via Nginx or any static host. Set `apiBaseUrl` in `environment.prod.ts` to your backend URL.
