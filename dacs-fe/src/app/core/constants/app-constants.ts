/**
 * Constantes generales de la aplicación
 */
export const APP_CONSTANTS = {
  APP_NAME: 'DACS Frontend',
  VERSION: '1.0.0',
  SUPPORTED_LANGUAGES: ['es', 'en'],
  DEFAULT_LANGUAGE: 'es',
  
  // Paginación
  PAGINATION: {
    DEFAULT_PAGE_SIZE: 10,
    PAGE_SIZE_OPTIONS: [5, 10, 25, 50, 100]
  },
  
  // Estados de carga
  LOADING_STATES: {
    IDLE: 'idle',
    LOADING: 'loading',
    SUCCESS: 'success',
    ERROR: 'error'
  },
  
  // Tiempos
  TIMEOUTS: {
    TOAST_DURATION: 3000,
    DEBOUNCE_SEARCH: 300,
    RETRY_ATTEMPTS: 3
  }
} as const;

/**
 * Mensajes de la aplicación
 */
export const APP_MESSAGES = {
  ERRORS: {
    NETWORK_ERROR: 'Error de conexión. Verifique su internet.',
    SERVER_ERROR: 'Error del servidor. Intente más tarde.',
    UNAUTHORIZED: 'No tiene permisos para realizar esta acción.',
    NOT_FOUND: 'Recurso no encontrado.',
    TIMEOUT: 'La operación tardó demasiado. Intente nuevamente.',
    UNKNOWN: 'Ha ocurrido un error inesperado.'
  },
  
  SUCCESS: {
    DATA_LOADED: 'Datos cargados correctamente.',
    DATA_SAVED: 'Datos guardados correctamente.',
    DATA_DELETED: 'Datos eliminados correctamente.',
    CONNECTION_OK: 'Conexión establecida correctamente.'
  },
  
  INFO: {
    LOADING: 'Cargando...',
    NO_DATA: 'No hay datos disponibles.',
    SEARCHING: 'Buscando...'
  }
} as const;
