import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, retry, shareReplay, switchMap } from 'rxjs/operators';
import { SteamUser, BibliotecaComparacionDto, mapUsuarioToSteamUser } from '../models/compare.models';

@Injectable({ providedIn: 'root' })
export class CompareService {
  private baseUrl = 'http://localhost:9001/bff';
  private userCache = new Map<string, Observable<SteamUser>>();

  constructor(private http: HttpClient) {}

  /**
   * GET /bff/usuarios/comparar/{id1}/con/{id2}...
   * For multiple users, we need to adapt the endpoint or call it pairwise
   * For now, using the 2-user endpoint as reference
   */
  getUsersByIds$(ids: string[]): Observable<SteamUser[]> {
    if (ids.length < 2) return of([]);
    
    // Call comparison endpoint to get user data
    const [id1, id2] = ids;
    const url = `${this.baseUrl}/usuarios/comparar/${id1}/con/${id2}`;
    
    return this.http.get<BibliotecaComparacionDto>(url).pipe(
      retry(1),
      map(response => {
        return (response.usuarios || []).map(u => mapUsuarioToSteamUser(u, response));
      }),
      catchError(this.handleError),
      shareReplay(1)
    );
  }

  /**
   * Search users by name (mock implementation - adapt to real BFF endpoint when available)
   */
  searchUsers$(q: string): Observable<SteamUser[]> {
    // TODO: implement real search endpoint when available
    // For now, return empty
    return of([]);
  }

  /**
   * Get single user by ID (with caching)
   */
  getUserById$(id: string): Observable<SteamUser> {
    if (!this.userCache.has(id)) {
      // Use the correct Steam API endpoints instead of the old /usuarios/steam endpoint
      const request$ = this.getUserProfileAndLibrary$(id).pipe(
        catchError(err => {
          console.warn('getUserById failed', err);
          return of({
            steamId: id,
            personaName: id,
            avatar: '',
            accountCreated: '',
            stats: { hours: 0, games: 0, uniques: 0 }
          } as SteamUser);
        }),
        shareReplay(1)
      );
      
      this.userCache.set(id, request$);
    }
    
    return this.userCache.get(id)!;
  }

  /**
   * Get user profile and library from Steam API endpoints
   * Detects if library is private by attempting to fetch games
   */
  private getUserProfileAndLibrary$(steamId: string): Observable<SteamUser> {
    const profileUrl = `${this.baseUrl}/steam/player-summaries`;
    const libraryUrl = `${this.baseUrl}/steam/user/${steamId}/games`;
    
    const params = new HttpParams().set('steamids', steamId);
    
    // Get profile first, then check library access
    return this.http.get<any>(profileUrl, { params }).pipe(
      retry(1),
      switchMap((profileResp: any) => {
        const player = profileResp?.response?.players?.[0];
        const communityVisibilityState = player?.communityvisibilitystate || 1;
        
        // Now try to get the library to verify access
        return this.http.get<any>(libraryUrl).pipe(
          map((libraryResp: any) => {
            const games = libraryResp?.response?.games || [];
            const hasLibraryAccess = games.length > 0;
            
            return {
              steamId: player?.steamid || steamId,
              personaName: player?.personaname || steamId,
              avatar: player?.avatarfull || '',
              accountCreated: player?.timecreated ? new Date(player.timecreated * 1000).toISOString() : '',
              countryCode: player?.loccountrycode || player?.localcountrycode || '',
              communityVisibilityState: communityVisibilityState,
              isPrivate: !hasLibraryAccess, // Biblioteca privada si no se pueden obtener juegos
              stats: { hours: 0, games: 0, uniques: 0 }
            } as SteamUser;
          }),
          catchError(() => {
            // Si falla obtener la biblioteca, es privada
            return of({
              steamId: player?.steamid || steamId,
              personaName: player?.personaname || steamId,
              avatar: player?.avatarfull || '',
              accountCreated: player?.timecreated ? new Date(player.timecreated * 1000).toISOString() : '',
              countryCode: player?.loccountrycode || player?.localcountrycode || '',
              communityVisibilityState: communityVisibilityState,
              isPrivate: true, // Biblioteca privada
              stats: { hours: 0, games: 0, uniques: 0 }
            } as SteamUser);
          })
        );
      }),
      catchError(() => of({
        steamId: steamId,
        personaName: steamId,
        avatar: '',
        accountCreated: '',
        countryCode: '',
        communityVisibilityState: 1,
        isPrivate: true, // Default to private on error
        stats: { hours: 0, games: 0, uniques: 0 }
      } as SteamUser))
    );
  }

  /**
   * Get comparison data for multiple users
   */
  compareUsers$(ids: string[]): Observable<BibliotecaComparacionDto> {
    if (ids.length < 2) {
      return of({ usuarios: [], juegosComunes: [], juegosUnicosPorUsuario: {} } as BibliotecaComparacionDto);
    }

    if (ids.length > 6) {
      console.warn('Máximo 6 usuarios permitidos para comparación');
      ids = ids.slice(0, 6);
    }

    // Use the comparar-multi endpoint for any number of users (2-6)
    const url = `${this.baseUrl}/usuarios/comparar-multi`;
    const params = new HttpParams().set('ids', ids.join(','));
    
    return this.http.get<BibliotecaComparacionDto>(url, { params }).pipe(
      retry(1),
      catchError(this.handleError),
      shareReplay(1)
    );
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
