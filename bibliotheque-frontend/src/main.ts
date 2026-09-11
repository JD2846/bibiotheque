import { enableProdMode, provideZoneChangeDetection, importProvidersFrom } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';


import { environment } from './environments/environment';
import { ErrorHandlerService } from './app/_core/services/error-handler.service';
import { LoadingService } from './app/_core/services/loading.service';
import { UserAuthService } from './app/_service/user-auth.service';
import { AuthGuard } from './app/_auth/auth.guard';
import { HTTP_INTERCEPTORS, provideHttpClient, withXhr, withInterceptorsFromDi } from '@angular/common/http';
import { AuthInterceptor } from './app/_auth/auth.interceptor';
import { BrowserModule, bootstrapApplication } from '@angular/platform-browser';
import { AppRoutingModule } from './app/app-routing.module';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { AppComponent } from './app/app.component';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
    providers: [
        importProvidersFrom(BrowserModule, AppRoutingModule, FormsModule, ReactiveFormsModule),
        ErrorHandlerService,
        LoadingService,
        UserAuthService,
        AuthGuard,
        {
            provide: HTTP_INTERCEPTORS,
            useClass: AuthInterceptor,
            multi: true
        },
        provideHttpClient(withXhr(), withInterceptorsFromDi())
    ]
})
  .catch(err => console.error(err));
