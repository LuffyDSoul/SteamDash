import { KeycloakConfig, KeycloakOnLoad, KeycloakPkceMethod } from 'keycloak-js';
import { environment } from '../../../environments/environment';

/**
 * Configuración de Keycloak
 */
export const keycloakConfig: KeycloakConfig = {
  url: environment.keycloak.url,
  realm: environment.keycloak.realm,
  clientId: environment.keycloak.clientId
};

/**
 * Opciones de inicialización de Keycloak
 * Configurado para evitar problemas de CSP usando standard flow
 */
export const keycloakInitOptions = {
  config: keycloakConfig,
  initOptions: {
    onLoad: 'login-required' as KeycloakOnLoad,
    pkceMethod: 'S256' as KeycloakPkceMethod,
    checkLoginIframe: false,
    enableLogging: true
  },
  enableBearerInterceptor: true,
  bearerExcludedUrls: [
    '/assets',
    'assets'
  ]
};
