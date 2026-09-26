import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { KeycloakService } from '../services/keycloak.service';

/**
 * Interceptor para manejo global de errores HTTP
 */
@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(
    private keycloakService: KeycloakService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        this.handleError(error);
        return throwError(() => error);
      })
    );
  }

  private handleError(error: HttpErrorResponse): void {
    let errorMessage = 'Ha ocurrido un error inesperado';
    let errorTitle = 'Error';

    if (error.error instanceof ErrorEvent) {
      // Error del cliente
      errorMessage = 'Error de conexión. Verifique su internet.';
      errorTitle = 'Error de Red';
    } else {
      // Error del servidor
      switch (error.status) {
        case 0:
          errorMessage = 'No se pudo conectar con el servidor.';
          errorTitle = 'Error de Conexión';
          break;
        case 400:
          errorMessage = error.error?.message || 'Solicitud inválida.';
          errorTitle = 'Error de Validación';
          break;
        case 401:
          errorMessage = 'Su sesión ha expirado. Por favor, inicie sesión nuevamente.';
          errorTitle = 'Sesión Expirada';
          this.keycloakService.logout();
          break;
        case 403:
          errorMessage = 'No tiene permisos para realizar esta acción.';
          errorTitle = 'Acceso Denegado';
          break;
        case 404:
          errorMessage = 'El recurso solicitado no fue encontrado.';
          errorTitle = 'No Encontrado';
          break;
        case 408:
          errorMessage = 'La operación tardó demasiado. Intente nuevamente.';
          errorTitle = 'Tiempo Agotado';
          break;
        case 500:
          errorMessage = 'Error interno del servidor. Intente más tarde.';
          errorTitle = 'Error del Servidor';
          break;
        case 502:
          errorMessage = 'El servidor no está disponible temporalmente.';
          errorTitle = 'Servidor No Disponible';
          break;
        case 503:
          errorMessage = 'El servicio está temporalmente fuera de línea.';
          errorTitle = 'Servicio No Disponible';
          break;
        default:
          errorMessage = error.error?.message || `Error ${error.status}: ${error.statusText}`;
          errorTitle = 'Error del Servidor';
      }
    }

    // Log del error para debugging
    console.error('HTTP Error:', {
      status: error.status,
      message: errorMessage,
      url: error.url,
      error: error.error
    });
  }
}
