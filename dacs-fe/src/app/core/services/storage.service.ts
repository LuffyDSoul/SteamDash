import { Injectable } from '@angular/core';

/**
 * Servicio para manejo de almacenamiento local y de sesión
 */
@Injectable({
  providedIn: 'root'
})
export class StorageService {
  private readonly PREFIX = 'dacs_';

  /**
   * Almacena un valor en localStorage
   */
  public setLocalItem(key: string, value: any): void {
    try {
      const serializedValue = JSON.stringify(value);
      localStorage.setItem(this.PREFIX + key, serializedValue);
    } catch (error) {
      console.error('Error storing item in localStorage:', error);
    }
  }

  /**
   * Obtiene un valor de localStorage
   */
  public getLocalItem<T>(key: string, defaultValue?: T): T | null {
    try {
      const item = localStorage.getItem(this.PREFIX + key);
      return item ? JSON.parse(item) : defaultValue || null;
    } catch (error) {
      console.error('Error retrieving item from localStorage:', error);
      return defaultValue || null;
    }
  }

  /**
   * Elimina un valor de localStorage
   */
  public removeLocalItem(key: string): void {
    localStorage.removeItem(this.PREFIX + key);
  }

  /**
   * Almacena un valor en sessionStorage
   */
  public setSessionItem(key: string, value: any): void {
    try {
      const serializedValue = JSON.stringify(value);
      sessionStorage.setItem(this.PREFIX + key, serializedValue);
    } catch (error) {
      console.error('Error storing item in sessionStorage:', error);
    }
  }

  /**
   * Obtiene un valor de sessionStorage
   */
  public getSessionItem<T>(key: string, defaultValue?: T): T | null {
    try {
      const item = sessionStorage.getItem(this.PREFIX + key);
      return item ? JSON.parse(item) : defaultValue || null;
    } catch (error) {
      console.error('Error retrieving item from sessionStorage:', error);
      return defaultValue || null;
    }
  }

  /**
   * Elimina un valor de sessionStorage
   */
  public removeSessionItem(key: string): void {
    sessionStorage.removeItem(this.PREFIX + key);
  }

  /**
   * Limpia todo el almacenamiento local
   */
  public clearLocalStorage(): void {
    const keys = Object.keys(localStorage);
    keys.forEach(key => {
      if (key.startsWith(this.PREFIX)) {
        localStorage.removeItem(key);
      }
    });
  }

  /**
   * Limpia todo el almacenamiento de sesión
   */
  public clearSessionStorage(): void {
    const keys = Object.keys(sessionStorage);
    keys.forEach(key => {
      if (key.startsWith(this.PREFIX)) {
        sessionStorage.removeItem(key);
      }
    });
  }

  /**
   * Verifica si existe una clave en localStorage
   */
  public hasLocalItem(key: string): boolean {
    return localStorage.getItem(this.PREFIX + key) !== null;
  }

  /**
   * Verifica si existe una clave en sessionStorage
   */
  public hasSessionItem(key: string): boolean {
    return sessionStorage.getItem(this.PREFIX + key) !== null;
  }

  /**
   * Obtiene el tamaño del almacenamiento local
   */
  public getLocalStorageSize(): number {
    let total = 0;
    for (const key in localStorage) {
      if (localStorage.hasOwnProperty(key) && key.startsWith(this.PREFIX)) {
        total += localStorage[key].length + key.length;
      }
    }
    return total;
  }

  /**
   * Obtiene el tamaño del almacenamiento de sesión
   */
  public getSessionStorageSize(): number {
    let total = 0;
    for (const key in sessionStorage) {
      if (sessionStorage.hasOwnProperty(key) && key.startsWith(this.PREFIX)) {
        total += sessionStorage[key].length + key.length;
      }
    }
    return total;
  }

  /**
   * Almacena datos de usuario
   */
  public setUserData(userData: any): void {
    this.setLocalItem('user_data', userData);
  }

  /**
   * Obtiene datos de usuario
   */
  public getUserData(): any {
    return this.getLocalItem('user_data');
  }

  /**
   * Almacena configuración de la aplicación
   */
  public setAppConfig(config: any): void {
    this.setLocalItem('app_config', config);
  }

  /**
   * Obtiene configuración de la aplicación
   */
  public getAppConfig(): any {
    return this.getLocalItem('app_config', {});
  }

  /**
   * Almacena preferencias del usuario
   */
  public setUserPreferences(preferences: any): void {
    this.setLocalItem('user_preferences', preferences);
  }

  /**
   * Obtiene preferencias del usuario
   */
  public getUserPreferences(): any {
    return this.getLocalItem('user_preferences', {});
  }
}
