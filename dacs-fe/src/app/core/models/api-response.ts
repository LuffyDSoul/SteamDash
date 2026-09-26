/**
 * Interfaz base para respuestas de la API
 */
export interface IApiResponse<T = any> {
  success: boolean;
  data: T;
  message?: string;
  timestamp: string;
  requestId?: string;
}

/**
 * Interfaz para respuestas de error
 */
export interface IApiError {
  error: boolean;
  errorCode: string;
  errorDescription: string;
  details?: any;
  timestamp: string;
  requestId?: string;
}

/**
 * Interfaz para paginación
 */
export interface IPagination {
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

/**
 * Interfaz para respuestas paginadas
 */
export interface IPaginatedResponse<T> extends IApiResponse<T[]> {
  pagination: IPagination;
}

/**
 * Interfaz para filtros de búsqueda
 */
export interface ISearchFilters {
  query?: string;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
  filters?: Record<string, any>;
}

/**
 * Interfaz para respuestas de estado
 */
export interface IStatusResponse {
  status: 'online' | 'offline' | 'maintenance';
  message: string;
  timestamp: string;
  version?: string;
}
