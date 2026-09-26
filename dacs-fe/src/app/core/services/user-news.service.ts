import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NewsItem {
  gid: string;
  title: string;
  url: string;
  isExternalUrl: boolean;
  author: string;
  contents: string;
  feedLabel: string;
  date: number;
  feedName: string;
  feedType: number;
  appId: number;
  isNew: boolean;
}

export interface GameNews {
  appId: number;
  gameName: string;
  gameImageUrl: string;
  latestNews: NewsItem[];
  olderNews: NewsItem[];
  totalNewsCount: number;
  lastNewsDate: number;
  showAllNews?: boolean; // Para controlar el desplegable en UI
}

export interface UserNewsResponse {
  steamId: string;
  gamesNews: GameNews[];
  totalGames: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

export interface GameDetails {
  appId: number;
  name: string;
  type: string;
  isFree: boolean;
  headerImage: string;
  shortDescription: string;
  detailedDescription: string;
  website: string;
  developers: string[];
  publishers: string[];
  priceOverview?: {
    currency: string;
    initial: number;
    finalPrice: number;
    discountPercent: number;
    finalFormatted: string;
  };
  categories: string[];
  genres: string[];
  screenshots: Array<{
    id: number;
    pathThumbnail: string;
    pathFull: string;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class UserNewsService {
  private bffUrl = 'http://localhost:9001/bff';

  constructor(private http: HttpClient) {}

  /**
   * Obtener noticias de TODOS los juegos del usuario (owned games)
   */
  getAllUserGamesNews(steamId: string, page: number = 0, pageSize: number = 25): Observable<UserNewsResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());
    
    const url = `${this.bffUrl}/user-news/${steamId}`;
    console.log('Calling BFF for ALL games with URL:', url, 'params:', params.toString());
    
    return this.http.get<UserNewsResponse>(url, { params });
  }

  /**
   * Obtener noticias de juegos JUGADOS RECIENTEMENTE
   */
  getRecentlyPlayedGamesNews(steamId: string, page: number = 0, pageSize: number = 25): Observable<UserNewsResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());
    
    const url = `${this.bffUrl}/user-news/${steamId}/recently-played`;
    console.log('Calling BFF for RECENTLY PLAYED with URL:', url, 'params:', params.toString());
    
    return this.http.get<UserNewsResponse>(url, { params });
  }

  /**
   * @deprecated Use getAllUserGamesNews or getRecentlyPlayedGamesNews instead
   */
  getUserGamesNews(steamId: string, page: number = 0, pageSize: number = 25): Observable<UserNewsResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('pageSize', pageSize.toString());
    
    const url = `${this.bffUrl}/user-news/${steamId}`;
    console.log('Calling BFF with URL:', url, 'params:', params.toString());
    
    return this.http.get<UserNewsResponse>(url, { params });
  }

  /**
   * Buscar detalles de un juego específico
   */
  searchGameDetails(appId: string): Observable<GameDetails> {
    return this.http.get<GameDetails>(`${this.bffUrl}/user-news/game-search/${appId}`);
  }

  /**
   * Obtener URL de imagen de un juego
   */
  getGameImageUrl(appId: number): string {
    return `https://cdn.akamai.steamstatic.com/steam/apps/${appId}/library_600x900.jpg`;
  }

  /**
   * Formatear fecha de noticia
   */
  formatNewsDate(timestamp: number): string {
    const date = new Date(timestamp * 1000);
    const now = new Date();
    const diffTime = Math.abs(now.getTime() - date.getTime());
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      return 'Hoy';
    } else if (diffDays === 1) {
      return 'Ayer';
    } else if (diffDays < 7) {
      return `Hace ${diffDays} días`;
    } else if (diffDays < 30) {
      const weeks = Math.floor(diffDays / 7);
      return `Hace ${weeks} ${weeks === 1 ? 'semana' : 'semanas'}`;
    } else if (diffDays < 365) {
      const months = Math.floor(diffDays / 30);
      return `Hace ${months} ${months === 1 ? 'mes' : 'meses'}`;
    } else {
      return date.toLocaleDateString('es-AR');
    }
  }

  /**
   * Truncar texto a longitud específica
   */
  truncateText(text: string, maxLength: number = 200): string {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  }
}
