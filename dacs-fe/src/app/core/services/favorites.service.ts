import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface FavoriteGame {
  appId: number;
  gameName: string;
  addedAt: number;
}

export interface UserFavoritesData {
  steamId: string;
  favoriteAppIds: number[];
  lastUpdated: number;
}

@Injectable({
  providedIn: 'root'
})
export class FavoritesService {
  private readonly STORAGE_KEY = 'steam_user_favorites';
  private currentSteamId: string | null = null;

  constructor() {}

  /**
   * Establecer el Steam ID del usuario actual
   */
  setCurrentSteamId(steamId: string): void {
    this.currentSteamId = steamId;
  }

  /**
   * Obtener el Steam ID del usuario actual
   */
  getCurrentSteamId(): string | null {
    return this.currentSteamId;
  }

  /**
   * Cargar datos de favoritos desde localStorage para un usuario específico
   */
  private loadUserFavorites(steamId: string): UserFavoritesData {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (!stored) {
        return {
          steamId,
          favoriteAppIds: [],
          lastUpdated: Date.now()
        };
      }
      
      const allData: Record<string, UserFavoritesData> = JSON.parse(stored);
      return allData[steamId] || {
        steamId,
        favoriteAppIds: [],
        lastUpdated: Date.now()
      };
    } catch (error) {
      console.error('Error loading favorites from localStorage', error);
      return {
        steamId,
        favoriteAppIds: [],
        lastUpdated: Date.now()
      };
    }
  }

  /**
   * Guardar favoritos en localStorage
   */
  private saveUserFavorites(steamId: string, favoriteAppIds: number[]): void {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      const allData: Record<string, UserFavoritesData> = stored ? JSON.parse(stored) : {};
      
      allData[steamId] = {
        steamId,
        favoriteAppIds,
        lastUpdated: Date.now()
      };
      
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(allData));
    } catch (error) {
      console.error('Error saving favorites to localStorage', error);
    }
  }

  /**
   * Obtener IDs de favoritos para el usuario actual
   */
  getFavoriteIds(): number[] {
    if (!this.currentSteamId) {
      return [];
    }
    
    const userData = this.loadUserFavorites(this.currentSteamId);
    return userData.favoriteAppIds;
  }

  /**
   * Verificar si un juego es favorito
   */
  isFavorite(appId: number): boolean {
    if (!this.currentSteamId) {
      return false;
    }
    
    const favoriteIds = this.getFavoriteIds();
    return favoriteIds.includes(appId);
  }

  /**
   * Agregar juego a favoritos
   */
  addFavorite(appId: number): void {
    if (!this.currentSteamId) {
      console.warn('No steam ID set, cannot add favorite');
      return;
    }
    
    const favoriteIds = this.getFavoriteIds();
    
    // No agregar duplicados
    if (favoriteIds.includes(appId)) {
      return;
    }
    
    favoriteIds.push(appId);
    this.saveUserFavorites(this.currentSteamId, favoriteIds);
  }

  /**
   * Remover juego de favoritos
   */
  removeFavorite(appId: number): void {
    if (!this.currentSteamId) {
      return;
    }
    
    const favoriteIds = this.getFavoriteIds().filter(id => id !== appId);
    this.saveUserFavorites(this.currentSteamId, favoriteIds);
  }

  /**
   * Toggle favorito (agregar si no existe, remover si existe)
   */
  toggleFavorite(appId: number): boolean {
    if (this.isFavorite(appId)) {
      this.removeFavorite(appId);
      return false; // Removido
    } else {
      this.addFavorite(appId);
      return true; // Agregado
    }
  }

  /**
   * Limpiar todos los favoritos del usuario actual
   */
  clearAll(): void {
    if (!this.currentSteamId) {
      return;
    }
    
    this.saveUserFavorites(this.currentSteamId, []);
  }
}
