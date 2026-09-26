import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseApiService } from './base-api.service';
import { IBibliotecaUsuario, IJuegoAleatorio, IRefreshJuegoResponse, IGameAchievements } from '../models/biblioteca-usuario.model';

/** Juegos recientemente jugados (últimas 2 semanas) */
export interface IRecentlyPlayedGame {
  appId: number;
  name: string;
  playtime2Weeks: number; // minutos últimas 2 semanas
}

/**
 * Servicio para gestionar la biblioteca de juegos de un usuario
 */
@Injectable({
  providedIn: 'root'
})
export class BibliotecaUsuarioService extends BaseApiService {

  /**
   * Obtiene la biblioteca de juegos de un usuario
   * @param steamId Steam ID del usuario
   * @param forceRefresh Si es true, agrega un parámetro para forzar actualización desde Steam
   */
  getBibliotecaUsuario(steamId: string, forceRefresh: boolean = false): Observable<IBibliotecaUsuario> {
    const params: any = {};
    if (forceRefresh) {
      params.refresh = 'true';
      params._t = Date.now(); // Cache buster adicional
    }
    return this.get<IBibliotecaUsuario>(`/usuarios/${steamId}/biblioteca`, params);
  }

  /**
   * Actualiza la información de un juego específico en la base de datos
   * Llama a appdetails, steamspy y obtiene la imagen de biblioteca
   * @param steamId Steam ID del usuario
   * @param appId App ID del juego
   */
  refreshJuego(steamId: string, appId: number): Observable<IRefreshJuegoResponse> {
    return this.post<IRefreshJuegoResponse>(
      `/usuarios/${steamId}/biblioteca/juegos/${appId}/refresh`,
      {}
    );
  }

  /**
   * Obtiene los juegos recientemente jugados (últimas 2 semanas) y su tiempo jugado
   * Endpoint esperado en el backend: GET /usuarios/{steamId}/biblioteca/recently-played
   * Retorna lista con appId y playtime2Weeks (minutos)
   */
  getRecentlyPlayedGames(steamId: string): Observable<IRecentlyPlayedGame[]> {
    return this.get<IRecentlyPlayedGame[]>(`/usuarios/${steamId}/biblioteca/recently-played`);
  }

  /**
   * Obtiene los juegos recientemente jugados directamente desde Steam API
   * Este método retorna TODOS los juegos jugados en las últimas 2 semanas,
   * incluyendo juegos prestados que no están en la biblioteca del usuario
   * @param steamId Steam ID del usuario
   * @returns Observable con lista de juegos y su tiempo jugado en minutos
   */
  getRecentlyPlayedGamesSteam(steamId: string): Observable<IRecentlyPlayedGame[]> {
    return this.get<IRecentlyPlayedGame[]>(`/usuarios/${steamId}/biblioteca/recently-played`);
  }

  /**
   * Obtiene un juego aleatorio de la biblioteca del usuario
   * @param steamId Steam ID del usuario
   * @param maxHoras Máximo de horas jugadas (null para solo no jugados)
   * @param tags Tags para filtrar (opcional)
   * @param excludeAppId AppId a excluir de la selección (para evitar repeticiones)
   */
  getJuegoAleatorio(steamId: string, maxHoras?: number, tags?: string[], excludeAppId?: number): Observable<IJuegoAleatorio> {
    let params: any = {};
    if (maxHoras !== undefined) {
      params.maxHoras = maxHoras.toString();
    }
    if (tags && tags.length > 0) {
      params.tags = tags.join(',');
    }
    if (excludeAppId !== undefined) {
      params.excludeAppId = excludeAppId.toString();
    }
    console.log('📡 Llamando a API con params:', params);
    return this.get<IJuegoAleatorio>(`/usuarios/${steamId}/biblioteca/juego-aleatorio`, params);
  }

  /**
   * Obtiene los logros de un juego para un usuario
   * @param steamId Steam ID del usuario
   * @param appId App ID del juego
   */
  getGameAchievements(steamId: string, appId: number): Observable<IGameAchievements> {
    return this.get<IGameAchievements>(`/usuarios/${steamId}/juegos/${appId}/logros`);
  }

  /**
   * Refresca forzadamente los logros de un juego desde Steam API
   * @param steamId Steam ID del usuario
   * @param appId App ID del juego
   */
  refreshGameAchievements(steamId: string, appId: number): Observable<IGameAchievements> {
    return this.post<IGameAchievements>(`/usuarios/${steamId}/juegos/${appId}/logros/refresh`, {});
  }

  /**
   * Sincroniza toda la biblioteca del usuario
   * Compara con la base de datos y actualiza los juegos que no tienen información completa
   * @param steamId Steam ID del usuario
   */
  syncBiblioteca(steamId: string): Observable<{ 
    total: number, 
    sincronizados: number, 
    errores: number,
    mensaje: string,
    juegosConError?: string[]
  }> {
    // Timeout de 2 horas para sincronización (puede tardar mucho con bibliotecas grandes)
    return this.post<{ 
      total: number, 
      sincronizados: number, 
      errores: number,
      mensaje: string,
      juegosConError?: string[]
    }>(`/usuarios/${steamId}/biblioteca/sync`, {}, 7200000);
  }

  /**
   * Actualiza TODOS los juegos de la biblioteca del usuario
   * Llama a refresh para cada juego para actualizar precios, imágenes, tags, logros
   * ADVERTENCIA: Operación larga que puede tardar varias horas con bibliotecas grandes
   * @param steamId Steam ID del usuario
   */
  refreshAllGames(steamId: string): Observable<{
    total: number,
    actualizados: number,
    errores: number,
    mensaje: string,
    juegosConError?: string[]
  }> {
    // Timeout de 2 horas para actualización completa
    return this.post<{
      total: number,
      actualizados: number,
      errores: number,
      mensaje: string,
      juegosConError?: string[]
    }>(`/usuarios/${steamId}/biblioteca/refresh-all`, {}, 7200000);
  }
}

