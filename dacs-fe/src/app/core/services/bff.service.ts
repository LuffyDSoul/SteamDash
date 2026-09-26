import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BaseApiService } from './base-api.service';
import { IUsuario, IBibliotecaComparacion, IBibliotecaJuego } from '../models/bff-models';
import { IApiResponse } from '../models/api-response';

/**
 * Servicio para interactuar con el BFF (Backend For Frontend)
 * Extiende BaseApiService para aprovechar manejo de errores y timeouts
 */
@Injectable({
  providedIn: 'root'
})
export class BffService extends BaseApiService {

  /**
   * Obtiene todos los usuarios
   */
  getAllUsuarios(): Observable<IUsuario[]> {
    return this.get<IUsuario[]>('/usuarios');
  }

  /**
   * Obtiene un usuario por ID
   */
  getUsuarioById(id: number): Observable<IUsuario> {
    return this.get<IUsuario>(`/usuarios/${id}`);
  }

  /**
   * Obtiene un usuario por Steam ID
   */
  getUsuarioBySteamId(steamId: string): Observable<IUsuario> {
    return this.get<IUsuario>(`/usuarios/steam/${steamId}`);
  }

  /**
   * Crea un nuevo usuario
   */
  createUsuario(usuario: IUsuario): Observable<IUsuario> {
    return this.post<IUsuario>('/usuarios', usuario);
  }

  /**
   * Actualiza un usuario existente
   */
  updateUsuario(id: number, usuario: IUsuario): Observable<IUsuario> {
    return this.put<IUsuario>(`/usuarios/${id}`, usuario);
  }

  /**
   * Elimina un usuario
   */
  deleteUsuario(id: number): Observable<void> {
    return this.delete<void>(`/usuarios/${id}`);
  }

  /**
   * Buscar aplicaciones por título (usa el endpoint /apps del BFF)
   */
  getAllApps(titulo?: string, desarrolladora?: string, publisher?: string, gratuito?: boolean, generos?: string[], categorias?: string[]) {
    const params: any = {};
    if (titulo) params.titulo = titulo;
    if (desarrolladora) params.desarrolladora = desarrolladora;
    if (publisher) params.publisher = publisher;
    if (gratuito != null) params.gratuito = gratuito;
    if (generos) params.generos = generos.join(',');
    if (categorias) params.categorias = categorias.join(',');
    const query = Object.keys(params).length ? '?' + Object.keys(params).map(k => `${k}=${encodeURIComponent(params[k])}`).join('&') : '';
    return this.get<any>(`/apps${query}`);
  }

  /**
   * Obtener noticias de un juego a través del BFF
   */
  getGameNews(appId: string, count?: number, maxLength?: number) {
    const params: any = {};
    if (count != null) params.count = count;
    if (maxLength != null) params.maxLength = maxLength;
    const query = Object.keys(params).length ? '?' + Object.keys(params).map(k => `${k}=${params[k]}`).join('&') : '';
    return this.get<any>(`/steam/game/${appId}/news${query}`);
  }
  
  /**
   * Obtener información del perfil de jugadores de Steam
   */
  getPlayerSummaries(steamIds: string): Observable<any> {
    return this.get<any>(`/steam/player-summaries?steamids=${steamIds}`);
  }

  /**
   * Obtener o actualizar un juego de biblioteca (upsert global)
   * @param appId ID del juego
   * @param steamId (Opcional) Steam ID del usuario para iniciar ingesta de logros
   */
  getBibliotecaJuego(appId: number, steamId?: string): Observable<IBibliotecaJuego> {
    const params = steamId ? { steamId } : {};
    return this.get<IBibliotecaJuego>(`/biblioteca/juego/${appId}`, params);
  }

  /**
   * Obtener aplicación por Steam App ID
   */
  getAppBySteamId(steamAppId: number): Observable<any> {
    return this.get<any>(`/apps/steam/${steamAppId}`);
  }
}
