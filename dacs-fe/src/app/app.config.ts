import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { KeycloakService } from 'keycloak-angular';
import { keycloakInitOptions } from './core/config/keycloak.config';

import { routes } from './app.routes';


function initializeKeycloak(keycloak: KeycloakService) {
  return () => {
    console.log('Inicializando Keycloak...');
    return keycloak.init(keycloakInitOptions).catch((error) => {
      console.error('Error en Keycloak:', error);
      // Si hay problemas de CSP, continuar sin Keycloak
      console.warn('Continuando sin Keycloak por problemas de configuración');
      return Promise.resolve();
    });
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(),
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),    
    KeycloakService,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService]
    }
  ]
};
