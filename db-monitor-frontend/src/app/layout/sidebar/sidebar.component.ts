// src/app/layout/sidebar/sidebar.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterLinkActive } from '@angular/router';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  badge?: string;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  @Input() collapsed = false;

  navItems: NavItem[] = [
    { label: 'Dashboard',    icon: 'dashboard',      route: '/dashboard' },
    { label: 'Live Events',  icon: 'bolt',           route: '/events' },
    { label: 'Metrics',      icon: 'bar_chart',      route: '/metrics' },
    { label: 'Applications', icon: 'apps',           route: '/applications' },
    { label: 'Alerts',       icon: 'notifications',  route: '/alerts' },
  ];

  bottomItems: NavItem[] = [
    { label: 'Settings', icon: 'settings', route: '/settings' },
  ];
}
