import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { BffService } from '../../core/services/bff.service';
import { SteamApiService, Game } from '../../core/models/steam/steam-api-response';
import { UserNewsService, UserNewsResponse, GameNews, GameDetails } from '../../core/services/user-news.service';
import { FavoritesService } from '../../core/services/favorites.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-news-home',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './news-home.component.html',
  styleUrls: ['./news-home.component.css']
})
export class NewsHomeComponent implements OnInit, OnDestroy {
  // Exponer Math para usar en template
  Math = Math;
  
  // Input de Steam ID del usuario
  steamId = '';
  
  // Información del perfil del usuario
  playerProfile: any = null;
  
  // Noticias de juegos del usuario
  userGamesNews: GameNews[] = [];
  allGamesNews: GameNews[] = []; // Todos los juegos cargados
  filteredGamesNews: GameNews[] = []; // Juegos filtrados por búsqueda
  cachedAllGames: GameNews[] = []; // Cache de TODOS los juegos
  cachedRecentlyPlayedGames: GameNews[] = []; // Cache de juegos jugados recientemente
  currentPage = 0;
  pageSize = 10; // Mostrar 10 juegos por página
  maxPages = 5; // Límite máximo de páginas
  loadingUserNews = false;
  loadingAllGamesInBackground = false; // Loading para cache en background
  
  // Búsqueda por nombre de juego
  gameNameSearchTerm = '';
  
  // Búsqueda de juego específico
  gameSearchTerm = '';
  searchedGameDetails: GameDetails | null = null;
  loadingGameSearch = false;
  
  // Filtros
  selectedFilter = 'recentlyPlayed'; // recentlyPlayed (por defecto), all, favorites
  
  // Estado
  error: string | null = null;
  private sub: Subscription | null = null;
  
  // Cache de imágenes de base de datos
  private imageCache: Map<number, string> = new Map();

  constructor(
    private bff: BffService,
    private router: Router,
    private steamApi: SteamApiService,
    private userNewsService: UserNewsService,
    private favoritesService: FavoritesService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    // El servicio de favoritos cargará automáticamente cuando se establezca el Steam ID
  }

  /**
   * Cargar noticias cuando se introduce un Steam ID
   * Por defecto carga jugados recientemente, y en background carga todos los juegos
   */
  loadUserGamesNews() {
    if (!this.steamId || this.steamId.trim().length === 0) {
      this.error = 'Por favor ingresa un Steam ID';
      return;
    }

    this.loadingUserNews = true;
    this.error = null;
    
    const trimmedSteamId = this.steamId.trim();
    console.log('=== Loading news for Steam ID:', trimmedSteamId, '===');
    
    // Limpiar caches al cargar nuevo Steam ID
    this.cachedAllGames = [];
    this.cachedRecentlyPlayedGames = [];
    
    // Resetear a filtro "recentlyPlayed" al cargar nuevo Steam ID
    this.selectedFilter = 'recentlyPlayed';
    console.log('Filter:', this.selectedFilter);
    
    // Establecer el Steam ID en el servicio de favoritos
    this.favoritesService.setCurrentSteamId(trimmedSteamId);
    
    // Cargar perfil del jugador
    this.loadPlayerProfile();

    // 1. Cargar JUGADOS RECIENTEMENTE primero (más rápido)
    const maxGames = this.maxPages * this.pageSize;
    this.userNewsService.getRecentlyPlayedGamesNews(trimmedSteamId, 0, maxGames)
      .subscribe({
        next: (response: UserNewsResponse) => {
          console.log('=== Recently played games received ===');
          console.log('Total games:', response.gamesNews.length);
          
          // Guardar juegos jugados recientemente
          this.allGamesNews = response.gamesNews;
          this.filteredGamesNews = [...response.gamesNews];
          this.cachedRecentlyPlayedGames = [...response.gamesNews];
          
          // Resetear página y mostrar primeros juegos
          this.currentPage = 0;
          this.updateDisplayedGames();
          
          this.loadingUserNews = false;
          
          console.log(`Loaded news for ${this.allGamesNews.length} recently played games`);
          
          // 2. Cargar TODOS los juegos en background (sin bloquear UI)
          this.loadAllGamesInBackground(trimmedSteamId, maxGames);
        },
        error: (err: any) => {
          console.error('Error loading recently played games news', err);
          this.loadingUserNews = false;
          this.error = 'No se encontraron juegos jugados recientemente. Intenta cambiar al filtro "Todos los Juegos".';
          
          // Si falla, intentar cargar todos los juegos directamente
          this.loadAllGamesInBackground(trimmedSteamId, maxGames);
        }
      });
  }
  
  /**
   * Cargar todos los juegos en background (sin bloquear UI)
   */
  private loadAllGamesInBackground(steamId: string, maxGames: number) {
    if (this.cachedAllGames.length > 0) {
      console.log('All games already cached, skipping background load');
      return;
    }
    
    console.log('=== Loading ALL games in background ===');
    this.loadingAllGamesInBackground = true;
    
    this.userNewsService.getAllUserGamesNews(steamId, 0, maxGames)
      .subscribe({
        next: (response: UserNewsResponse) => {
          console.log('=== All games loaded in background ===');
          console.log('Total games:', response.gamesNews.length);
          
          // Cachear TODOS los juegos para uso futuro
          this.cachedAllGames = [...response.gamesNews];
          this.loadingAllGamesInBackground = false;
          
          console.log(`Cached ${this.cachedAllGames.length} total games in background`);
        },
        error: (err: any) => {
          console.error('Error loading all games in background', err);
          this.loadingAllGamesInBackground = false;
        }
      });
  }
  
  /**
   * Actualizar juegos mostrados según la página actual
   */
  private updateDisplayedGames() {
    const startIndex = this.currentPage * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.userGamesNews = this.filteredGamesNews.slice(startIndex, endIndex);
    console.log(`Showing games ${startIndex + 1} to ${Math.min(endIndex, this.filteredGamesNews.length)} of ${this.filteredGamesNews.length}`);
  }
  
  /**
   * Cargar siguiente página
   */
  loadNextPage() {
    if (!this.hasMoreGames()) {
      return;
    }
    
    this.currentPage++;
    this.updateDisplayedGames();
    
    // Scroll al inicio de la página - usar múltiples métodos para mayor compatibilidad
    setTimeout(() => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
    }, 0);
  }
  
  /**
   * Cargar página anterior
   */
  loadPreviousPage() {
    if (this.currentPage === 0) {
      return;
    }
    
    this.currentPage--;
    this.updateDisplayedGames();
    
    // Scroll al inicio de la página - usar múltiples métodos para mayor compatibilidad
    setTimeout(() => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
    }, 0);
  }
  
  /**
   * Verificar si hay más juegos para mostrar (respetando límite de páginas)
   */
  hasMoreGames(): boolean {
    const hasMoreData = (this.currentPage + 1) * this.pageSize < this.filteredGamesNews.length;
    const withinPageLimit = this.currentPage + 1 < this.maxPages;
    return hasMoreData && withinPageLimit;
  }
  
  /**
   * Ir a la primera página
   */
  goToFirstPage() {
    if (this.currentPage === 0) {
      return;
    }
    
    this.currentPage = 0;
    this.updateDisplayedGames();
    
    // Scroll al inicio de la página
    setTimeout(() => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
    }, 0);
  }
  
  /**
   * Ir a la última página
   */
  goToLastPage() {
    const maxPage = Math.min(
      Math.ceil(this.filteredGamesNews.length / this.pageSize) - 1,
      this.maxPages - 1
    );
    
    if (this.currentPage === maxPage) {
      return;
    }
    
    this.currentPage = maxPage;
    this.updateDisplayedGames();
    
    // Scroll al inicio de la página
    setTimeout(() => {
      window.scrollTo({ top: 0, behavior: 'smooth' });
      document.documentElement.scrollTop = 0;
      document.body.scrollTop = 0;
    }, 0);
  }
  
  /**
   * Cargar noticias solo de juegos favoritos
   */
  private loadNewsForFavorites() {
    const favoriteIds = this.favoritesService.getFavoriteIds();
    
    if (favoriteIds.length === 0) {
      this.userGamesNews = [];
      this.allGamesNews = [];
      this.loadingUserNews = false;
      // No establecer error, el mensaje se mostrará en el HTML
      return;
    }
    
    console.log('Loading news for', favoriteIds.length, 'favorite games');
    
    // Cargar noticias para cada App ID favorito
    const newsPromises = favoriteIds.map(appId => {
      return new Promise<GameNews | null>((resolve) => {
        this.bff.getGameNews(String(appId), 5, 300)
          .subscribe({
            next: (response: any) => {
              if (response && response.appnews && response.appnews.newsitems && response.appnews.newsitems.length > 0) {
                // Intentar obtener nombre del juego
                this.userNewsService.searchGameDetails(String(appId))
                  .subscribe({
                    next: (details: GameDetails) => {
                      const gameNews: GameNews = {
                        appId: appId,
                        gameName: details.name,
                        gameImageUrl: details.headerImage,
                        latestNews: response.appnews.newsitems.slice(0, 2).map((item: any) => ({
                          gid: item.gid,
                          title: item.title,
                          url: item.url,
                          isExternalUrl: item.is_external_url || false,
                          author: item.author,
                          contents: item.contents,
                          feedLabel: item.feedlabel || '',
                          date: item.date,
                          feedName: item.feedname || '',
                          feedType: item.feed_type || 0,
                          appId: appId,
                          isNew: false
                        })),
                        olderNews: response.appnews.newsitems.slice(2).map((item: any) => ({
                          gid: item.gid,
                          title: item.title,
                          url: item.url,
                          isExternalUrl: item.is_external_url || false,
                          author: item.author,
                          contents: item.contents,
                          feedLabel: item.feedlabel || '',
                          date: item.date,
                          feedName: item.feedname || '',
                          feedType: item.feed_type || 0,
                          appId: appId,
                          isNew: false
                        })),
                        totalNewsCount: response.appnews.newsitems.length,
                        lastNewsDate: response.appnews.newsitems[0]?.date || 0,
                        showAllNews: false
                      };
                      resolve(gameNews);
                    },
                    error: () => {
                      // Si falla obtener detalles, usar app ID como nombre
                      const gameNews: GameNews = {
                        appId: appId,
                        gameName: `Juego ${appId}`,
                        gameImageUrl: `https://cdn.cloudflare.steamstatic.com/steam/apps/${appId}/header.jpg`,
                        latestNews: response.appnews.newsitems.slice(0, 2).map((item: any) => ({
                          gid: item.gid,
                          title: item.title,
                          url: item.url,
                          isExternalUrl: item.is_external_url || false,
                          author: item.author,
                          contents: item.contents,
                          feedLabel: item.feedlabel || '',
                          date: item.date,
                          feedName: item.feedname || '',
                          feedType: item.feed_type || 0,
                          appId: appId,
                          isNew: false
                        })),
                        olderNews: response.appnews.newsitems.slice(2).map((item: any) => ({
                          gid: item.gid,
                          title: item.title,
                          url: item.url,
                          isExternalUrl: item.is_external_url || false,
                          author: item.author,
                          contents: item.contents,
                          feedLabel: item.feedlabel || '',
                          date: item.date,
                          feedName: item.feedname || '',
                          feedType: item.feed_type || 0,
                          appId: appId,
                          isNew: false
                        })),
                        totalNewsCount: response.appnews.newsitems.length,
                        lastNewsDate: response.appnews.newsitems[0]?.date || 0,
                        showAllNews: false
                      };
                      resolve(gameNews);
                    }
                  });
              } else {
                resolve(null);
              }
            },
            error: () => resolve(null)
          });
      });
    });
    
    Promise.all(newsPromises).then(results => {
      const validGames = results.filter(g => g !== null) as GameNews[];
      
      // Invertir el orden de los juegos para favoritos
      this.allGamesNews = validGames.reverse();
      this.filteredGamesNews = [...this.allGamesNews];
      
      this.currentPage = 0;
      this.updateDisplayedGames();
      this.loadingUserNews = false;
      console.log('Loaded news for', validGames.length, 'favorite games (reversed order)');
    });
  }
  
  /**
   * Cargar perfil del jugador
   */
  loadPlayerProfile() {
    this.bff.getPlayerSummaries(this.steamId.trim())
      .subscribe({
        next: (response: any) => {
          if (response && response.response && response.response.players && response.response.players.length > 0) {
            this.playerProfile = response.response.players[0];
            console.log('Player profile loaded:', this.playerProfile);
          }
        },
        error: (err: any) => {
          console.error('Error loading player profile', err);
        }
      });
  }

  /**
   * Toggle para mostrar todas las noticias de un juego
   */
  toggleShowAllNews(game: GameNews) {
    game.showAllNews = !game.showAllNews;
  }

  /**
   * Formatear fecha de noticia
   */
  formatDate(timestamp: number): string {
    return this.userNewsService.formatNewsDate(timestamp);
  }

  /**
   * Truncar texto
   */
  truncate(text: string, length: number = 200): string {
    return this.userNewsService.truncateText(text, length);
  }

  /**
   * Abrir noticia en nueva pestaña
   */
  openNews(url: string) {
    window.open(url, '_blank');
  }

  /**
   * Verificar si una noticia es reciente (menos de 30 días)
   */
  isNewsRecent(timestamp: number): boolean {
    const now = Math.floor(Date.now() / 1000);
    const diff = now - timestamp;
    const thirtyDaysInSeconds = 30 * 24 * 60 * 60;
    return diff < thirtyDaysInSeconds;
  }
  
  /**
   * Sanitizar HTML para prevenir XSS
   */
  sanitizeHtml(html: string): SafeHtml {
    return this.sanitizer.sanitize(1, html) || '';
  }
  
  /**
   * Obtener URL de imagen header como fallback
   */
  getGameHeaderImage(appId: number): string {
    return `https://cdn.cloudflare.steamstatic.com/steam/apps/${appId}/header.jpg`;
  }
  
  /**
   * Obtener URL de imagen del juego con fallback
   * Si no hay imagen de la API de noticias, intenta buscar en base de datos primero
   */
  getGameImage(game: GameNews): string {
    // Si hay imagen de la API de noticias, usarla
    if (game.gameImageUrl) {
      return game.gameImageUrl;
    }
    
    // Si ya tenemos la imagen en cache (de BD), usarla
    if (this.imageCache.has(game.appId)) {
      const cached = this.imageCache.get(game.appId);
      if (cached) {
        return cached;
      }
    }
    
    // Si no hay en cache, intentar cargar de BD en background
    if (!this.imageCache.has(game.appId)) {
      this.loadImageFromDatabase(game.appId);
    }
    
    // Mientras tanto, usar Steam CDN como temporal
    return this.getGameHeaderImage(game.appId);
  }
  
  /**
   * Cargar imagen desde la base de datos
   */
  private loadImageFromDatabase(appId: number) {
    // Marcar como "en proceso" para evitar múltiples llamadas
    this.imageCache.set(appId, '');
    
    this.bff.getAppBySteamId(appId).subscribe({
      next: (app) => {
        if (app && app.headerImageUrl) {
          this.imageCache.set(appId, app.headerImageUrl);
          // Forzar actualización de la vista
          this.userGamesNews = [...this.userGamesNews];
        } else {
          this.imageCache.set(appId, '');
        }
      },
      error: () => {
        this.imageCache.set(appId, '');
      }
    });
  }
  
  /**
   * Manejar error de carga de imagen
   * Intenta con base de datos primero, luego Steam CDN, finalmente placeholder
   */
  onImageError(event: any, game: GameNews) {
    const currentSrc = event.target.src;
    const headerImage = this.getGameHeaderImage(game.appId);
    const placeholderImage = 'https://via.placeholder.com/600x900?text=Sin+Imagen';
    
    // Si ya estamos en placeholder, no hacer nada más
    if (currentSrc === placeholderImage) {
      return;
    }
    
    // Si tenemos imagen de BD en cache, usarla
    if (this.imageCache.has(game.appId)) {
      const dbImage = this.imageCache.get(game.appId);
      if (dbImage && dbImage !== '' && currentSrc !== dbImage) {
        event.target.src = dbImage;
        return;
      }
    }
    
    // Si aún no intentamos con Steam CDN, intentar
    if (currentSrc !== headerImage) {
      event.target.src = headerImage;
      return;
    }
    
    // Si Steam CDN falló y no tenemos en cache, buscar en BD
    if (!this.imageCache.has(game.appId)) {
      this.bff.getAppBySteamId(game.appId).subscribe({
        next: (app) => {
          if (app && app.headerImageUrl && app.headerImageUrl !== currentSrc) {
            this.imageCache.set(game.appId, app.headerImageUrl);
            event.target.src = app.headerImageUrl;
          } else {
            this.imageCache.set(game.appId, '');
            event.target.src = placeholderImage;
          }
        },
        error: () => {
          this.imageCache.set(game.appId, '');
          event.target.src = placeholderImage;
        }
      });
      return;
    }
    
    // Último recurso: placeholder
    event.target.src = placeholderImage;
  }
  
  /**
   * Aplicar búsqueda por nombre de juego
   */
  applyGameNameSearch() {
    const searchTerm = this.gameNameSearchTerm.toLowerCase().trim();
    
    if (searchTerm === '') {
      // Sin búsqueda, mostrar todos los juegos del filtro actual
      this.filteredGamesNews = [...this.allGamesNews];
    } else {
      // Filtrar juegos por nombre
      this.filteredGamesNews = this.allGamesNews.filter(game => 
        game.gameName.toLowerCase().includes(searchTerm)
      );
    }
    
    // Resetear a primera página y actualizar vista
    this.currentPage = 0;
    this.updateDisplayedGames();
  }
  
  /**
   * Limpiar búsqueda por nombre
   */
  clearGameNameSearch() {
    this.gameNameSearchTerm = '';
    this.applyGameNameSearch();
  }
  
  /**
   * Aplicar filtro
   */
  applyFilter(filter: string) {
    this.selectedFilter = filter;
    this.currentPage = 0; // Resetear página al cambiar filtro
    
    // Limpiar búsqueda al cambiar filtro
    this.gameNameSearchTerm = '';
    
    if (!this.steamId) {
      return;
    }
    
    if (filter === 'favorites') {
      // Cargar favoritos
      this.loadNewsForFavorites();
      
    } else if (filter === 'recentlyPlayed') {
      // Restaurar juegos jugados recientemente desde cache
      if (this.cachedRecentlyPlayedGames.length > 0) {
        console.log('Restaurando juegos jugados recientemente desde cache (instantáneo)');
        this.allGamesNews = [...this.cachedRecentlyPlayedGames];
        this.filteredGamesNews = [...this.cachedRecentlyPlayedGames];
        this.error = null;
        this.currentPage = 0;
        this.updateDisplayedGames();
      } else {
        // Cargar desde backend si no hay cache
        console.log('Cargando juegos jugados recientemente desde backend...');
        this.loadingUserNews = true;
        const maxGames = this.maxPages * this.pageSize;
        this.userNewsService.getRecentlyPlayedGamesNews(this.steamId.trim(), 0, maxGames)
          .subscribe({
            next: (response: UserNewsResponse) => {
              this.allGamesNews = response.gamesNews;
              this.filteredGamesNews = [...response.gamesNews];
              this.cachedRecentlyPlayedGames = [...response.gamesNews];
              this.error = null;
              this.currentPage = 0;
              this.updateDisplayedGames();
              this.loadingUserNews = false;
            },
            error: (err: any) => {
              console.error('Error loading recently played games', err);
              this.loadingUserNews = false;
              this.error = 'No se encontraron juegos jugados recientemente. Intenta cambiar al filtro \"Todos los Juegos\".';
              this.allGamesNews = [];
              this.filteredGamesNews = [];
              this.updateDisplayedGames();
            }
          });
      }
      
    } else if (filter === 'all') {
      // Restaurar TODOS los juegos desde cache
      if (this.cachedAllGames.length > 0) {
        console.log('Restaurando todos los juegos desde cache (instantáneo)');
        this.allGamesNews = [...this.cachedAllGames];
        this.filteredGamesNews = [...this.cachedAllGames];
        this.error = null;
        this.currentPage = 0;
        this.updateDisplayedGames();
      } else {
        // Cargar desde backend si no hay cache
        console.log('Cargando todos los juegos desde backend...');
        this.loadingUserNews = true;
        const maxGames = this.maxPages * this.pageSize;
        this.userNewsService.getAllUserGamesNews(this.steamId.trim(), 0, maxGames)
          .subscribe({
            next: (response: UserNewsResponse) => {
              this.allGamesNews = response.gamesNews;
              this.filteredGamesNews = [...response.gamesNews];
              this.cachedAllGames = [...response.gamesNews];
              this.error = null;
              this.currentPage = 0;
              this.updateDisplayedGames();
              this.loadingUserNews = false;
            },
            error: (err: any) => {
              console.error('Error loading all games', err);
              this.loadingUserNews = false;
              this.error = 'Error al cargar todos los juegos';
            }
          });
      }
    }
  }
  
  /**
   * Toggle favorito para un juego
   */
  toggleFavorite(game: GameNews) {
    const isNowFavorite = this.favoritesService.toggleFavorite(game.appId);
    console.log(isNowFavorite ? 'Added to favorites:' : 'Removed from favorites:', game.gameName);
  }
  
  /**
   * Verificar si un juego es favorito
   */
  isFavorite(appId: number): boolean {
    return this.favoritesService.isFavorite(appId);
  }
  
  /**
   * Buscar y mostrar noticias de un juego específico por App ID
   */
  searchGame() {
    if (!this.gameSearchTerm || this.gameSearchTerm.trim().length === 0) {
      this.error = 'Por favor ingresa un App ID para buscar';
      return;
    }

    const appId = this.gameSearchTerm.trim();
    this.loadingGameSearch = true;
    this.error = null;
    this.searchedGameDetails = null;
    this.userGamesNews = [];
    this.allGamesNews = [];

    // Cargar noticias del juego específico
    this.bff.getGameNews(appId, 10, 300)
      .subscribe({
        next: (response: any) => {
          if (response && response.appnews && response.appnews.newsitems && response.appnews.newsitems.length > 0) {
            // Cargar detalles del juego en paralelo
            this.userNewsService.searchGameDetails(appId)
              .subscribe({
                next: (details: GameDetails) => {
                  // Crear objeto GameNews con las noticias del juego
                  const gameNews: GameNews = {
                    appId: parseInt(appId),
                    gameName: details.name,
                    gameImageUrl: details.headerImage,
                    latestNews: response.appnews.newsitems.map((item: any) => ({
                      gid: item.gid,
                      title: item.title,
                      url: item.url,
                      isExternalUrl: item.is_external_url || false,
                      author: item.author,
                      contents: item.contents,
                      feedLabel: item.feedlabel || '',
                      date: item.date,
                      feedName: item.feedname || '',
                      feedType: item.feed_type || 0,
                      appId: parseInt(appId),
                      isNew: false
                    })),
                    olderNews: [],
                    totalNewsCount: response.appnews.newsitems.length,
                    lastNewsDate: response.appnews.newsitems[0]?.date || 0,
                    showAllNews: false
                  };
                  
                  this.allGamesNews = [gameNews];
                  this.userGamesNews = [gameNews];
                  this.currentPage = 0;
                  this.loadingGameSearch = false;
                  console.log(`Loaded ${gameNews.latestNews.length} news for game:`, details.name);
                },
                error: (err: any) => {
                  // Si no se pueden cargar los detalles, usar solo appId
                  const gameNews: GameNews = {
                    appId: parseInt(appId),
                    gameName: `Juego ${appId}`,
                    gameImageUrl: `https://cdn.cloudflare.steamstatic.com/steam/apps/${appId}/header.jpg`,
                    latestNews: response.appnews.newsitems.map((item: any) => ({
                      gid: item.gid,
                      title: item.title,
                      url: item.url,
                      isExternalUrl: item.is_external_url || false,
                      author: item.author,
                      contents: item.contents,
                      feedLabel: item.feedlabel || '',
                      date: item.date,
                      feedName: item.feedname || '',
                      feedType: item.feed_type || 0,
                      appId: parseInt(appId),
                      isNew: false
                    })),
                    olderNews: [],
                    totalNewsCount: response.appnews.newsitems.length,
                    lastNewsDate: response.appnews.newsitems[0]?.date || 0,
                    showAllNews: false
                  };
                  
                  this.allGamesNews = [gameNews];
                  this.userGamesNews = [gameNews];
                  this.currentPage = 0;
                  this.loadingGameSearch = false;
                  console.log(`Loaded ${gameNews.latestNews.length} news for app:`, appId);
                }
              });
          } else {
            this.loadingGameSearch = false;
            this.error = 'No se encontraron noticias para este juego';
          }
        },
        error: (err: any) => {
          console.error('Error searching game news', err);
          this.loadingGameSearch = false;
          this.error = 'No se encontró el juego o error al buscar';
        }
      });
  }
  
  ngOnDestroy(): void {
    if (this.sub) this.sub.unsubscribe();
  }
}
