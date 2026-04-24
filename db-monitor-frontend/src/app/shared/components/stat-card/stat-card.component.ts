// src/app/shared/components/stat-card/stat-card.component.ts
import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stat-card.component.html',
  styleUrls: ['./stat-card.component.scss']
})
export class StatCardComponent {
  @Input() title = '';
  @Input() value: string | number = 0;
  @Input() subtitle = '';
  @Input() icon = 'analytics';
  @Input() trend: 'up' | 'down' | 'neutral' = 'neutral';
  @Input() trendValue = '';
  @Input() variant: 'default' | 'success' | 'warning' | 'danger' | 'info' = 'default';
  @Input() loading = false;
}
