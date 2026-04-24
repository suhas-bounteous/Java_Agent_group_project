# DB Monitor Frontend — Complete Setup Guide

## Tech Stack
- **Angular 16** (standalone components + NgModules hybrid)
- **Chart.js 4** via `ng2-charts` — all dashboard charts
- **Angular Material 16** — UI components, tables, dialogs
- **RxJS** — reactive polling & state management
- **SCSS** — custom dark theme design system

---

## Step 1 — Create the Angular Project

```bash
npm install -g @angular/cli@16
ng new db-monitor-frontend --style=scss --routing=true --strict=false
cd db-monitor-frontend
```

---

## Step 2 — Install Dependencies

```bash
# Chart.js + Angular wrapper
npm install chart.js ng2-charts

# Angular Material + CDK + Animations
npm install @angular/material@16 @angular/cdk@16 @angular/animations@16

# Icons (Material Icons)
npm install @angular/material-icons

# HTTP & utilities (already in Angular core, just confirming)
# HttpClientModule is part of @angular/common/http
```

---

## Step 3 — Add Google Fonts + Material Icons to index.html

Edit `src/index.html` — replace `<head>` section with the one provided in this guide.

---

## Step 4 — File Creation Order

Create files in this exact order to avoid import errors:

### 4.1 Models (src/app/core/models/)
1. `event.model.ts`
2. `metrics.model.ts`
3. `dashboard.model.ts`

### 4.2 Services (src/app/core/services/)
4. `api.service.ts`
5. `metrics.service.ts`
6. `events.service.ts`
7. `dashboard.service.ts`
8. `alert.service.ts`

### 4.3 Interceptors (src/app/core/interceptors/)
9. `error.interceptor.ts`

### 4.4 Shared Components (src/app/shared/components/)
10. `stat-card/stat-card.component.ts`
11. `stat-card/stat-card.component.html`
12. `stat-card/stat-card.component.scss`
13. `alert-badge/alert-badge.component.ts`
14. `loading-spinner/loading-spinner.component.ts`

### 4.5 Layout (src/app/layout/)
15. `header/header.component.ts` + html + scss
16. `sidebar/sidebar.component.ts` + html + scss

### 4.6 Feature Components (src/app/features/)
17. `dashboard/dashboard.component.ts` + html + scss
18. `events/events.component.ts` + html + scss
19. `metrics/metrics.component.ts` + html + scss
20. `applications/applications.component.ts` + html + scss
21. `alerts/alerts.component.ts` + html + scss

### 4.7 Root files
22. `app.module.ts`
23. `app-routing.module.ts`
24. `app.component.ts` + html + scss
25. `styles.scss` (global)
26. `environments/environment.ts`

---

## Step 5 — Backend API Endpoints

The backend runs on `http://localhost:8081`. The current backend only has:
- `POST /events` — receive events from agent
- `POST /metrics` — receive metrics from agent

**You need to add GET endpoints to the backend** for the dashboard to read data.
Add these to your Spring Boot backend (see `BACKEND_ADDITIONS.md`).

---

## Step 6 — Run

```bash
ng serve --open
# App runs at http://localhost:4200
```

---

## Project Folder Structure (Final)

```
src/
├── app/
│   ├── core/
│   │   ├── models/
│   │   │   ├── event.model.ts
│   │   │   ├── metrics.model.ts
│   │   │   └── dashboard.model.ts
│   │   ├── services/
│   │   │   ├── api.service.ts
│   │   │   ├── metrics.service.ts
│   │   │   ├── events.service.ts
│   │   │   ├── dashboard.service.ts
│   │   │   └── alert.service.ts
│   │   └── interceptors/
│   │       └── error.interceptor.ts
│   ├── shared/
│   │   └── components/
│   │       ├── stat-card/
│   │       ├── alert-badge/
│   │       └── loading-spinner/
│   ├── features/
│   │   ├── dashboard/
│   │   ├── events/
│   │   ├── metrics/
│   │   ├── applications/
│   │   └── alerts/
│   ├── layout/
│   │   ├── header/
│   │   └── sidebar/
│   ├── app.module.ts
│   ├── app-routing.module.ts
│   ├── app.component.ts
│   ├── app.component.html
│   └── app.component.scss
├── environments/
│   ├── environment.ts
│   └── environment.prod.ts
└── styles.scss
```
