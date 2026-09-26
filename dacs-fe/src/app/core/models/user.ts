/**
 * Interfaz para datos de usuario
 */
export interface IUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  avatar?: string;
  roles: string[];
  permissions: string[];
  isActive: boolean;
  lastLogin?: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Interfaz para datos de perfil de usuario
 */
export interface IUserProfile extends IUser {
  preferences: IUserPreferences;
  settings: IUserSettings;
}

/**
 * Interfaz para preferencias de usuario
 */
export interface IUserPreferences {
  language: string;
  theme: 'light' | 'dark' | 'auto';
  timezone: string;
  dateFormat: string;
  notifications: INotificationSettings;
}

/**
 * Interfaz para configuración de notificaciones
 */
export interface INotificationSettings {
  email: boolean;
  push: boolean;
  sms: boolean;
  marketing: boolean;
}

/**
 * Interfaz para configuración de usuario
 */
export interface IUserSettings {
  dashboard: IDashboardSettings;
  table: ITableSettings;
  general: IGeneralSettings;
}

/**
 * Interfaz para configuración del dashboard
 */
export interface IDashboardSettings {
  defaultView: 'grid' | 'list';
  refreshInterval: number;
  showCharts: boolean;
  widgets: string[];
}

/**
 * Interfaz para configuración de tablas
 */
export interface ITableSettings {
  pageSize: number;
  showFilters: boolean;
  defaultSort: string;
  showColumns: string[];
}

/**
 * Interfaz para configuración general
 */
export interface IGeneralSettings {
  autoSave: boolean;
  confirmActions: boolean;
  showTooltips: boolean;
}
