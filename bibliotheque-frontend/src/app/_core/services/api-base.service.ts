import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, retry } from 'rxjs/operators';
import { ErrorHandlerService } from './error-handler.service';

type ApiParamValue = string | number | boolean | undefined | null;

export abstract class ApiBaseService {
  protected readonly apiUrl = 'http://localhost:8080';

  protected constructor(
    protected http: HttpClient,
    protected errorHandler: ErrorHandlerService
  ) { }

  protected get<T>(endpoint: string, params?: object): Observable<T> {
    return this.http.get<T>(this.buildUrl(endpoint), { params: this.buildParams(params) }).pipe(
      retry(1),
      catchError(error => this.errorHandler.handleError(error))
    );
  }

  protected post<T>(endpoint: string, body: unknown): Observable<T> {
    return this.http.post<T>(this.buildUrl(endpoint), body).pipe(
      catchError(error => this.errorHandler.handleError(error))
    );
  }

  protected put<T>(endpoint: string, body: unknown): Observable<T> {
    return this.http.put<T>(this.buildUrl(endpoint), body).pipe(
      catchError(error => this.errorHandler.handleError(error))
    );
  }

  protected patch<T>(endpoint: string, body: unknown): Observable<T> {
    return this.http.patch<T>(this.buildUrl(endpoint), body).pipe(
      catchError(error => this.errorHandler.handleError(error))
    );
  }

  protected delete<T>(endpoint: string): Observable<T> {
    return this.http.delete<T>(this.buildUrl(endpoint)).pipe(
      catchError(error => this.errorHandler.handleError(error))
    );
  }

  protected buildParams(params?: object): HttpParams {
    let httpParams = new HttpParams();

    if (!params) {
      return httpParams;
    }

    Object.entries(params).forEach(([key, rawValue]) => {
      const value = rawValue as ApiParamValue;
      if (value !== undefined && value !== null && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return httpParams;
  }

  private buildUrl(endpoint: string): string {
    return `${this.apiUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`;
  }
}
