import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, retry, shareReplay } from 'rxjs/operators';

import { Game } from '../models/library.models';

@Injectable({ providedIn: 'root' })
export class LibraryCompareService {
  private baseUrl = '/bff';
  private cache = new Map<string, Observable<any>>();

  constructor(private http: HttpClient) {}

  /**
   * Devuelve la lista inicial de usuarios comparados (vacío o precargado desde localStorage)
   */
  getInitialComparedUsers() {
    try {
      const raw = localStorage.getItem('library_compare_initial_users');
      if (raw) {
        const parsed = JSON.parse(raw) as any[];
        return of(parsed);
      }
    } catch (e) {
      // ignore parse errors
    }
    // Default empty slots: one editable slot shown to user
    return of([]);
  }

  /**
   * Obtiene información pública de un usuario de Steam mediante BFF -> conector
   * Retorna los campos personaName, avatarFull, timeCreated, locCountryCode y communityVisibilityState
   */
  getUserProfile(steamId: string) {
    if (!steamId) return of(null);
    const url = `${this.baseUrl}/steam/player-summaries`;
    const params = new HttpParams().set('steamids', steamId);
    console.log('getUserProfile: calling', url, 'with steamId:', steamId);
    return this.http.get<any>(url, { params }).pipe(
      map((resp: any) => {
        console.log('getUserProfile: response received:', resp);
        const player = resp?.response?.players?.[0];
        if (!player) {
          console.warn('getUserProfile: no player found in response');
          return null;
        }
        const profile = {
          steamId: player.steamid || steamId,
          personaName: player.personaname || null,
          avatarUrl: player.avatarfull || null,
          createdAt: player.timecreated || null,
          locCountryCode: player.localcountrycode || player.loccountrycode || null,
          communityVisibilityState: player.communityvisibilitystate || null
        };
        console.log('getUserProfile: parsed profile:', profile);
        return profile;
      }),
      catchError(err => {
        console.error('getUserProfile: error occurred:', err);
        return this.handleError(err);
      }),
      shareReplay(1)
    );
  }

  /**
   * Obtiene la biblioteca de un usuario mediante BFF -> conector
   */
  getUserLibrary(steamId: string) {
    if (!steamId) return of([]);
    const url = `${this.baseUrl}/steam/user/${steamId}/games`;
    console.log('getUserLibrary: calling', url);
    return this.http.get<any>(url).pipe(
      map((resp: any) => {
        console.log('getUserLibrary: response received:', resp);
        const games = resp?.response?.games || [];
        console.log('getUserLibrary: parsed games count:', games.length);
        return games;
      }),
      catchError(err => {
        console.error('getUserLibrary: error occurred:', err);
        return of([]);
      }),
      shareReplay(1)
    );
  }

  /**
   * GET /api/games?query=&tags=tag1,tag2&adult=true&avatars=a1,a2
   * For now, adapt to BFF endpoints - this may need custom implementation
   */
  getGames$(q?: string, tags?: string[], includeAdult?: boolean, avatars?: string[]): Observable<Game[]> {
    // TODO: implement when BFF has a games endpoint
    // For now, return empty
    return of([]);
  }

  /**
   * GET /api/tags
   * Returns available tags for filtering
   */
  getTags$(): Observable<string[]> {
    // TODO: implement when BFF has tags endpoint
    // Mock popular tags for now
    return of([
      'Action', 'RPG', 'FPS', 'Adventure', 'Multiplayer',
      'Co-op', 'Singleplayer', 'Strategy', 'Indie', 'Horror',
      'Open World', 'Puzzle', 'Simulation', 'Sports', 'Racing'
    ]);
  }

  /**
   * GET /api/avatars
   * Returns available user avatars for filtering
   */
  getAvatars$(): Observable<string[]> {
    // TODO: implement when BFF has avatars endpoint
    return of([]);
  }

  /**
   * Helper method to parse games from BFF response
   */
  private parseGame(data: any): Game {
    // Extract tags from nested structure
    const tags = Array.isArray(data.tags)
      ? data.tags.map((t: any) => typeof t === 'string' ? t : (t as any).tag || '').filter(Boolean)
      : [];

    // Detect adult content from tags or explicit field
    const hasAdultContent = tags.some((t: string) => 
      ['Sexual Content', 'Nudity', 'Adult', 'NSFW', 'Mature', 'Hentai'].includes(t)
    );

    return {
      appId: data.appId || 0,
      name: data.name || 'Unknown',
      tags,
      // Use headerImage if available, fallback to imgIconUrl
      headerImage: data.headerImage || data.imgIconUrl || '',
      hours: data.hours || data.playtime_hours || 0,
      owners: data.cantidadCopias || data.owners,
      hasAdultContent
    };
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMsg = 'An error occurred';
    
    if (error.error instanceof ErrorEvent) {
      errorMsg = `Client error: ${error.error.message}`;
    } else {
      errorMsg = `Server error (${error.status}): ${error.message}`;
    }
    
    console.error(errorMsg);
    return throwError(() => new Error(errorMsg));
  }
}
