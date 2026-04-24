// src/main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { importProvidersFrom } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { NgChartsModule, NgChartsConfiguration } from 'ng2-charts';

import { AppRoutingModule } from './app/app-routing.module';
import { ErrorInterceptor } from './app/core/interceptors/error.interceptor';

bootstrapApplication(AppComponent, {
  providers: [
    importProvidersFrom(
      BrowserModule,
      BrowserAnimationsModule,
      HttpClientModule,
      AppRoutingModule,
      NgChartsModule
    ),
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    },
    {
      provide: NgChartsConfiguration,
      useValue: { generateColors: false }
    }
  ]
}).catch(err => console.error(err));
