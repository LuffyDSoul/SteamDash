import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { BaseApiService } from './base-api.service';
import { IRequestTest } from '../models/irequest-test';
import { ITestResponse } from '../models/iresponse';
import { IApiResponse, IStatusResponse } from '../models/api-response';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { environment } from '../../../environments/environment';

/**
 * Servicio para operaciones de API específicas de la aplicación
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService extends BaseApiService {

  /**
   * Verifica el estado de conexión con el backend
   */
  getPing(): Observable<string> {
    const url = `${environment.backendForFrontendUrl}/ping`;
    console.log('ApiService - Ping URL:', url);
    
    return this.http.get<string>(url, { 
      responseType: 'text' as 'json',
      headers: this.defaultHeaders
    }).pipe(
      map(response => response || 'OK'),
      catchError((error) => {
        console.error('ApiService - Ping Error:', error);
        throw new Error('Error de conexión con el backend');
      })
    );
  }

  /**
   * Obtiene el estado detallado del sistema
   */
  getSystemStatus(): Observable<IStatusResponse> {
    return this.get<IStatusResponse>(API_ENDPOINTS.BFF.HEALTH);
  }

  /**
   * Obtiene datos de prueba desde el archivo JSON local
   */
  getTest(): Observable<ITestResponse> {
    // Para archivos locales, usamos HttpClient directamente
    return this.http.get<ITestResponse>(API_ENDPOINTS.ASSETS.TEST_DATA).pipe(
      catchError(() => {
        throw new Error('Error al cargar datos de prueba');
      })
    );
  }

  /**
   * Obtiene datos de prueba desde el backend
   */
  getTestFromBackend(): Observable<IApiResponse<ITestResponse>> {
    return this.get<IApiResponse<ITestResponse>>('test');
  }

  /**
   * Envía datos de prueba al backend
   */
  sendTestData(data: IRequestTest): Observable<IApiResponse<any>> {
    return this.post<IApiResponse<any>>('test', data);
  }

  /**
   * Obtiene datos del dashboard
   */
  getDashboardData(): Observable<IApiResponse<any>> {
    return this.get<IApiResponse<any>>(API_ENDPOINTS.BFF.DASHBOARD);
  }

  /**
   * Obtiene información del usuario actual
   */
  getCurrentUser(): Observable<IApiResponse<any>> {
    return this.get<IApiResponse<any>>(API_ENDPOINTS.BFF.USER);
  }
}
