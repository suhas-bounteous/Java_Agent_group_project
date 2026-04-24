// src/app/app-routing.module.ts
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'events',
    loadComponent: () =>
      import('./features/events/events.component').then(m => m.EventsComponent)
  },
  {
    path: 'metrics',
    loadComponent: () =>
      import('./features/metrics/metrics.component').then(m => m.MetricsComponent)
  },
  {
    path: 'applications',
    loadComponent: () =>
      import('./features/applications/applications.component').then(m => m.ApplicationsComponent)
  },
  {
    path: 'alerts',
    loadComponent: () =>
      import('./features/alerts/alerts.component').then(m => m.AlertsComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { scrollPositionRestoration: 'top' })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
