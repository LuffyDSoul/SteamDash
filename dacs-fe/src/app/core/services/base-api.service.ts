import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, timer } from 'rxjs';
import { catchError, retry, timeout, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS, HTTP_HEADERS, HTTP_TIMEOUTS } from '../constants/api-endpoints';
import { IApiResponse, IApiError, ISearchFilters } from '../models/api-response';
import { APP_MESSAGES } from '../constants/app-constants';

/**
 * Servicio base para operaciones HTTP
 */
@Injectable({
  providedIn: 'root'
})
export class BaseApiService {
  protected readonly baseUrl: string;
  protected readonly defaultHeaders: HttpHeaders;

  constructor(protected http: HttpClient) {
    // Asegurar que la URL base sea absoluta
    this.baseUrl = environment.backendForFrontendUrl.startsWith('http') 
      ? environment.backendForFrontendUrl 
      : `http://${environment.backendForFrontendUrl}`;
    
    this.defaultHeaders = new HttpHeaders({
      [HTTP_HEADERS.CONTENT_TYPE]: HTTP_HEADERS.APPLICATION_JSON,
      [HTTP_HEADERS.ACCEPT]: HTTP_HEADERS.APPLICATION_JSON
    });
  }

  /**
   * Realiza una petición GET
   */
  protected get<T>(endpoint: string, params?: any, customTimeout?: number): Observable<T> {
    const url = this.buildUrl(endpoint);
    const httpParams = this.buildParams(params);
    const timeoutValue = customTimeout || HTTP_TIMEOUTS.DEFAULT;

    return this.http.get<T>(url, {
      headers: this.defaultHeaders,
      params: httpParams
    }).pipe(
      timeout(timeoutValue),
      retry(2),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Realiza una petición POST
   */
  protected post<T>(endpoint: string, data: any, customTimeout?: number): Observable<T> {
    const url = this.buildUrl(endpoint);
    const timeoutValue = customTimeout || HTTP_TIMEOUTS.DEFAULT;

    return this.http.post<T>(url, data, {
      headers: this.defaultHeaders
    }).pipe(
      timeout(timeoutValue),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Realiza una petición PUT
   */
  protected put<T>(endpoint: string, data: any, customTimeout?: number): Observable<T> {
    const url = this.buildUrl(endpoint);
    const timeoutValue = customTimeout || HTTP_TIMEOUTS.DEFAULT;

    return this.http.put<T>(url, data, {
      headers: this.defaultHeaders
    }).pipe(
      timeout(timeoutValue),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Realiza una petición DELETE
   */
  protected delete<T>(endpoint: string, customTimeout?: number): Observable<T> {
    const url = this.buildUrl(endpoint);
    const timeoutValue = customTimeout || HTTP_TIMEOUTS.DEFAULT;

    return this.http.delete<T>(url, {
      headers: this.defaultHeaders
    }).pipe(
      timeout(timeoutValue),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Realiza una petición PATCH
   */
  protected patch<T>(endpoint: string, data: any, customTimeout?: number): Observable<T> {
    const url = this.buildUrl(endpoint);
    const timeoutValue = customTimeout || HTTP_TIMEOUTS.DEFAULT;

    return this.http.patch<T>(url, data, {
      headers: this.defaultHeaders
    }).pipe(
      timeout(timeoutValue),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Construye la URL completa
   */
  private buildUrl(endpoint: string): string {
    // Si el endpoint ya es una URL completa, usarlo directamente
    if (endpoint.startsWith('http')) {
      return endpoint;
    }
    
    // Si el endpoint comienza con '/', no añadir barra adicional
    if (endpoint.startsWith('/')) {
      return `${this.baseUrl}${endpoint}`;
    }
    
    // Construir URL normal
    return `${this.baseUrl}/${endpoint}`.replace(/\/+/g, '/');
  }

  /**
   * Construye los parámetros HTTP
   */
  private buildParams(params: any): HttpParams {
    let httpParams = new HttpParams();
    
    if (params) {
      Object.keys(params).forEach(key => {
        if (params[key] !== null && params[key] !== undefined) {
          httpParams = httpParams.set(key, params[key].toString());
        }
      });
    }
    
    return httpParams;
  }

  /**
   * Maneja errores HTTP
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage: string = APP_MESSAGES.ERRORS.UNKNOWN;
    let errorCode = 'UNKNOWN_ERROR';

    if (error.error instanceof ErrorEvent) {
      // Error del cliente
      errorMessage = APP_MESSAGES.ERRORS.NETWORK_ERROR;
      errorCode = 'CLIENT_ERROR';
    } else {
      // Error del servidor
      switch (error.status) {
        case 0:
          errorMessage = APP_MESSAGES.ERRORS.NETWORK_ERROR;
          errorCode = 'NETWORK_ERROR';
          break;
        case 400:
          errorMessage = error.error?.message || 'Solicitud inválida';
          errorCode = 'BAD_REQUEST';
          break;
        case 401:
          errorMessage = APP_MESSAGES.ERRORS.UNAUTHORIZED;
          errorCode = 'UNAUTHORIZED';
          break;
        case 404:
          errorMessage = APP_MESSAGES.ERRORS.NOT_FOUND;
          errorCode = 'NOT_FOUND';
          break;
        case 408:
          errorMessage = APP_MESSAGES.ERRORS.TIMEOUT;
          errorCode = 'TIMEOUT';
          break;
        case 500:
          errorMessage = APP_MESSAGES.ERRORS.SERVER_ERROR;
          errorCode = 'SERVER_ERROR';
          break;
        default:
          errorMessage = error.error?.message || APP_MESSAGES.ERRORS.SERVER_ERROR;
          errorCode = `HTTP_${error.status}`;
      }
    }

    const apiError: IApiError = {
      error: true,
      errorCode,
      errorDescription: errorMessage,
      details: error.error,
      timestamp: new Date().toISOString(),
      requestId: this.generateRequestId()
    };

    console.error('API Error:', apiError);
    return throwError(() => apiError);
  }

  /**
   * Genera un ID único para la petición
   */
  private generateRequestId(): string {
    return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Verifica el estado de la conexión
   */
  public checkConnection(): Observable<boolean> {
    return this.get<string>(API_ENDPOINTS.BFF.PING, null, HTTP_TIMEOUTS.PING).pipe(
      map(() => true),
      catchError(() => throwError(() => false))
    );
  }
}
