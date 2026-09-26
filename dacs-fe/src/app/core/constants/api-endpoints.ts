/**
 * Constantes para endpoints de la API
 */
export const API_ENDPOINTS = {
  // Backend for Frontend
  BFF: {
    BASE_URL: 'bff',
    PING: 'ping',
    HEALTH: 'health',
    USER: 'user',
    DASHBOARD: 'dashboard'
  },
  
  // Assets locales
  ASSETS: {
    TEST_DATA: 'assets/json/test.json',
    CONFIG: 'assets/config/app-config.json'
  }
} as const;

/**
 * Constantes para headers HTTP
 */
export const HTTP_HEADERS = {
  CONTENT_TYPE: 'Content-Type',
  AUTHORIZATION: 'Authorization',
  ACCEPT: 'Accept',
  APPLICATION_JSON: 'application/json',
  BEARER: 'Bearer'
} as const;

/**
 * Timeouts para requests HTTP
 */
export const HTTP_TIMEOUTS = {
  DEFAULT: 30000,    // 30 segundos
  PING: 5000,        // 5 segundos
  UPLOAD: 60000,     // 60 segundos
  SYNC: 7200000      // 2 horas (para sincronización de biblioteca completa)
} as const;
