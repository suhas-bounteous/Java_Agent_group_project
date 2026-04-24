// src/app/app.module.ts
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ErrorInterceptor } from './core/interceptors/error.interceptor';

// ng2-charts requires this global registration
import { NgChartsModule, NgChartsConfiguration } from 'ng2-charts';

@NgModule({
  declarations: [
    // AppComponent is standalone so NOT declared here
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    AppRoutingModule,
    NgChartsModule,
  ],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: ErrorInterceptor,
      multi: true
    },
    {
      provide: NgChartsConfiguration,
      useValue: { generateColors: false }
    }
  ],
  bootstrap: []   // bootstrap via AppComponent standalone below
})
export class AppModule {}
