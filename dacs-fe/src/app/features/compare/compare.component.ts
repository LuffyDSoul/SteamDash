import { Component, OnInit, signal, computed, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';

import { UserCompareCardComponent } from './components/user-compare-card/user-compare-card.component';
import { AddUserSlotComponent } from './components/add-user-slot/add-user-slot.component';
import { GameCardComponent } from './components/game-card/game-card.component';
import { CompareService } from './services/compare.service';
import { ContentFilterService } from '../../core/services/content-filter.service';
import { SteamUser, BibliotecaComparacionDto, mapUsuarioToSteamUser, Game } from './models/compare.models';

const STORAGE_KEY = 'compare-users';

type SortMode = 'alpha-asc' | 'alpha-desc' | 'copies-desc' | 'copies-asc';

@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule, UserCompareCardComponent, AddUserSlotComponent, GameCardComponent],
  templateUrl: './compare.component.html',
  styleUrls: ['./compare.component.css']
})
export class CompareComponent implements OnInit {
  protected users = signal<SteamUser[]>([]);
  protected loading = signal<boolean>(false);
  protected error = signal<string | null>(null);
  protected comparison = signal<BibliotecaComparacionDto | null>(null);
  protected sortMode = signal<SortMode>('copies-desc');
  protected tagFilter = signal<string>('');
  protected nameFilter = signal<string>('');
  protected showTagFilterInput = signal<boolean>(false);
  protected selectedCommonTags = signal<Set<string>>(new Set());
  protected selectedUserFilters = signal<Set<string>>(new Set()); // Filtro por usuarios específicos
  protected showTagDropdown = signal<boolean>(false);
  // Control de expansión de listas por clave
  protected expandedLists = signal<Record<string, boolean>>({});
  // Filtro Atlas (NSFW) - solo en memoria, no persistente
  protected atlasFilterActive = signal<boolean>(false);
  // Progress bar (0-100)
  protected progress = signal<number>(0);

  protected canAddMore = computed(() => this.users().length < 6);
  protected hasPrivateProfiles = computed(() => this.users().some(u => u.isPrivate));
  protected hiddenGamesCount = signal<number>(0);

  constructor(
    private svc: CompareService,
    private route: ActivatedRoute,
    private contentFilter: ContentFilterService
  ) {
    // Persist users to sessionStorage on change
    effect(() => {
      const u = this.users();
      if (u.length > 0) {
        const ids = u.map(x => x.steamId);
        sessionStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
      }
    });

    // Cuando loading() se active, inyectar script de Tenor para el embed y
    // proporcionar fallback si el embed no se renderiza.
    effect(() => {
      if (this.loading()) {
        // Defer slightly so the DOM node for the embed is present
        setTimeout(() => this.injectTenorScriptWithFallback(), 0);
      }
    });
  }

  ngOnInit(): void {
    // Load from sessionStorage only (no defaults)
    this.loadInitialUsers();
  }

  protected trackByUserId(index: number, user: SteamUser): string {
    return user.steamId;
  }

  // Tenor embed handling
  private tenorScriptInjected = false;

  private injectTenorScriptWithFallback() {
    try {
      if (this.tenorScriptInjected) return;

      // If the script is already present in the page, mark injected and return
      if (document.querySelector('script[src="https://tenor.com/embed.js"]')) {
        this.tenorScriptInjected = true;
      } else {
        const s = document.createElement('script');
        s.src = 'https://tenor.com/embed.js';
        s.async = true;
        document.body.appendChild(s);
        this.tenorScriptInjected = true;
      }

      // After a short timeout, check if Tenor replaced the embed div; if not, use a GIF fallback
      setTimeout(() => {
        const embedDiv = document.querySelector('.tenor-gif-embed[data-postid="9262676895723379299"]');
        if (!embedDiv) return;

        // If Tenor didn't render (no iframe/img added), replace with direct GIF
        const rendered = !!embedDiv.querySelector('iframe, img');
        if (!rendered) {
          embedDiv.innerHTML = '';
          const img = document.createElement('img');
          // Prefer a local asset copy if available (place the GIF at /assets/lcpjZfMORLb.gif)
          img.src = '/assets/lcpjZfMORLb.gif';
          img.alt = 'Cargando...';
          img.className = 'loading-gif';
          // Additional minor styling to ensure good appearance
          img.style.display = 'block';
          embedDiv.appendChild(img);
        }
      }, 1200);
    } catch (e) {
      // ignore script injection errors
      console.warn('Tenor embed injection failed', e);
    }
  }

  private loadInitialUsers() {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    let ids: string[] = [];

    if (stored) {
      try {
        ids = JSON.parse(stored);
      } catch {}
    }

    // Fetch user data for each stored ID
    ids.slice(0, 6).forEach(id => {
      this.svc.getUserById$(id).subscribe({
        next: user => {
          this.users.update(current => {
            // Avoid duplicates
            if (current.some(u => u.steamId === id)) return current;
            return [...current, user];
          });
        },
        error: err => {
          console.warn(`Failed to load user ${id}:`, err);
          this.error.set(`Error al cargar usuario ${id}`);
        }
      });
    });

    // Don't auto-compare on load
  }

  protected onAddUser(steamId: string) {
    if (!steamId || this.users().length >= 6) return;

    // Check if already exists
    if (this.users().some(u => u.steamId === steamId)) {
      this.error.set('Este usuario ya está en la comparación');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.svc.getUserById$(steamId).subscribe({
      next: user => {
        this.users.update(current => [...current, user]);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(`No se pudo agregar el usuario: ${err.message}`);
        this.loading.set(false);
      }
    });
  }

  protected onEditUser(oldSteamId: string, newSteamId: string) {
    if (!newSteamId || newSteamId === oldSteamId) return;

    // Check if new ID already exists
    if (this.users().some(u => u.steamId === newSteamId)) {
      this.error.set('Este usuario ya está en la comparación');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.svc.getUserById$(newSteamId).subscribe({
      next: user => {
        this.users.update(current => 
          current.map(u => u.steamId === oldSteamId ? user : u)
        );
        this.loading.set(false);
      },
      error: err => {
        this.error.set(`No se pudo actualizar el usuario: ${err.message}`);
        this.loading.set(false);
      }
    });
  }

  protected onCompare() {
    const ids = this.users().map(u => u.steamId);
    
    if (ids.length < 2) {
      this.error.set('Agrega al menos 2 usuarios para comparar');
      return;
    }

    this.fetchComparison(ids);
  }

  protected onRemoveUser(steamId: string) {
    this.users.update(current => current.filter(u => u.steamId !== steamId));
    
    // Clear comparison (user needs to click Compare again)
    this.comparison.set(null);

    // Announce removal for accessibility
    const remaining = this.users();
    this.announceChange(`Usuario eliminado. ${remaining.length} usuarios en comparación.`);
  }

  private fetchComparison(ids: string[]) {
    if (ids.length < 2) {
      this.comparison.set(null);
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.progress.set(0);

    // Simulate progress while waiting for response
    const progressInterval = setInterval(() => {
      this.progress.update(p => {
        if (p >= 90) return p; // Cap at 90% until real response
        return p + 5; // Incremento más lento (5% cada intervalo)
      });
    }, 300); // Intervalo más largo (300ms)

    this.svc.compareUsers$(ids).subscribe({
      next: result => {
        clearInterval(progressInterval);
        this.progress.set(100);
        
        console.log('Comparison result received:', result);
        this.comparison.set(result);
        
        // Update user stats from comparison and detect private libraries
        this.users.update(current => 
          current.map(u => {
            const userData = result.usuarios?.find(ru => 
              ru.steamId === u.steamId || ru.personaName === u.personaName
            );
            if (userData) {
              const mappedUser = mapUsuarioToSteamUser(userData, result);
              console.log(`Mapped user ${mappedUser.personaName}:`, mappedUser.stats);
              
              // Si el usuario no tiene juegos (ni comunes ni únicos), su biblioteca es privada
              // También verificar si totalHorasJugadas es 0 (otro indicador de biblioteca privada)
              const hasNoGames = mappedUser.stats.games === 0;
              const hasNoHours = (userData.totalHorasJugadas || 0) === 0;
              const isLibraryPrivate = hasNoGames || (hasNoGames && hasNoHours);
              
              return {
                ...mappedUser,
                countryCode: u.countryCode, // Preserve country code
                communityVisibilityState: u.communityVisibilityState, // Preserve visibility
                isPrivate: isLibraryPrivate // Marcar como privado si no tiene juegos visibles
              };
            }
            console.warn('User not found in comparison result:', u.steamId);
            return u;
          })
        );
        
        console.log('Updated users:', this.users());
        
        // Reset loading state after brief delay to show 100%
        setTimeout(() => {
          this.loading.set(false);
          this.progress.set(0);
        }, 300);
      },
      error: err => {
        clearInterval(progressInterval);
        this.progress.set(0);
        console.error('Comparison error:', err);
        
        // Enhanced error message for user-friendly display
        let errorMessage = 'Error al comparar bibliotecas.';
        
        if (err.message.includes('500') || err.status === 500) {
          errorMessage = 'Error al comparar bibliotecas. Es posible que alguna biblioteca sea privada o no esté disponible.';
        } else if (err.message.includes('privada') || err.message.includes('private')) {
          const userNames = this.users().map(u => u.personaName).join(', ');
          errorMessage = `La lista de juegos de uno o más usuarios es privada. Los usuarios deben configurar su biblioteca como pública en Steam. Usuarios: ${userNames}`;
        } else if (err.status === 404) {
          errorMessage = 'No se pudo encontrar el servicio de comparación. Verifica que el backend esté corriendo.';
        } else if (err.status === 0) {
          errorMessage = 'No se pudo conectar con el servidor. Verifica tu conexión.';
        } else {
          errorMessage = `Error al comparar bibliotecas. ${err.message || 'Intenta nuevamente.'}`;
        }
        
        this.error.set(errorMessage);
        this.loading.set(false);
      }
    });
  }

  /**
   * Detecta si un juego es +18 usando el filtro Atlas compartido
   */
  private isAdultContent(name: string, tags: string[]): boolean {
    return this.contentFilter.isAdultContent(name, tags);
  }

  /**
   * Construye lista completa de todos los juegos de todos los usuarios
   * con información de cuántos y quiénes lo tienen
   */
  protected getAllGames(): Game[] {
    const comp = this.comparison();
    if (!comp) return [];

    console.log('Building all games from comparison:', comp);
    const gamesMap = new Map<number, Game>();
    const currentUsers = this.users();
    console.log('Current users:', currentUsers);

    // Procesar juegos comunes
    (comp.juegosComunes || []).forEach(game => {
      const appId = game.appId || 0;
      if (!appId) return;
      
      const gameName = game.name || '';
      const imgVertical = (game as any)['img-vertical'] || (game as any).imgVertical || null;
      const headerImage = game.headerImage || null;
      
      // Filtrar juegos sin imagen válida (indica que no están disponibles en Steam Store actualmente)
      const hasValidImage = (imgVertical && imgVertical !== 'null' && imgVertical.trim() !== '') || 
                           (headerImage && headerImage !== 'null' && headerImage.trim() !== '');
      
      if (!hasValidImage) {
        console.log(`Skipping game ${appId} (${gameName}) - no valid image (not available in Steam Store)`);
        return;
      }

      const tags = Array.isArray(game.tags)
        ? game.tags.map(t => typeof t === 'string' ? t : (t as any).tag || '').filter(Boolean)
        : [];

      // cantidadCopias indica cuántos usuarios tienen este juego
      const copiesCount = game.cantidadCopias || 0;

      // Encontrar qué usuarios tienen este juego basándose en tiempoJugadoPorUsuario
      const playTimeMap = game.tiempoJugadoPorUsuario || {};
      const owners = currentUsers
        .filter(u => {
          // El backend puede usar personaName o steamId como key
          return playTimeMap[u.personaName] !== undefined || playTimeMap[u.steamId] !== undefined;
        })
        .map(u => ({
          steamId: u.steamId,
          avatar: u.avatar,
          personaName: u.personaName
        }));

      // Solo usar imagen si realmente existe y no es null/vacía (ya validado arriba)
      const finalImage = (imgVertical && imgVertical !== 'null') ? imgVertical : 
                        (headerImage && headerImage !== 'null') ? headerImage : '';

      gamesMap.set(appId, {
        appId,
        name: gameName,
        tags,
        headerImage: finalImage,
        imgVertical: finalImage,
        price: game.price || null,
        storeUrl: game.storeUrl || `https://store.steampowered.com/app/${appId}`,
        hours: 0,
        owners: copiesCount, // Usar cantidadCopias directamente
        ownersList: owners,
        hasAdultContent: this.isAdultContent(gameName, tags)
      });
    });

    console.log('Processed common games:', gamesMap.size);

    // Procesar juegos únicos de cada usuario
    Object.entries(comp.juegosUnicosPorUsuario || {}).forEach(([userKey, games]) => {
      console.log(`Processing unique games for key: ${userKey}, count: ${games.length}`);
      // userKey puede ser steamId o personaName, intentar encontrar el usuario
      const user = currentUsers.find(u => u.steamId === userKey || u.personaName === userKey);
      if (!user) {
        console.warn(`User not found for key: ${userKey}`);
        return;
      }

      (games || []).forEach(game => {
        const appId = game.appId || 0;
        if (!appId) return;

        // Si ya existe (fue procesado en comunes), skip para evitar duplicados
        if (gamesMap.has(appId)) {
          console.log(`Game ${game.name} already in map, skipping`);
          return;
        }
        
        const gameName = game.name || '';
        const imgVertical = (game as any)['img-vertical'] || (game as any).imgVertical || null;
        const headerImage = game.headerImage || null;
        
        // Filtrar juegos sin imagen válida (indica que no están disponibles en Steam Store actualmente)
        const hasValidImage = (imgVertical && imgVertical !== 'null' && imgVertical.trim() !== '') || 
                             (headerImage && headerImage !== 'null' && headerImage.trim() !== '');
        
        if (!hasValidImage) {
          console.log(`Skipping unique game ${appId} (${gameName}) - no valid image (not available in Steam Store)`);
          return;
        }

        const tags = Array.isArray(game.tags)
          ? game.tags.map(t => typeof t === 'string' ? t : (t as any).tag || '').filter(Boolean)
          : [];

        // Solo usar imagen si realmente existe y no es null/vacía (ya validado arriba)
        const finalImage = (imgVertical && imgVertical !== 'null') ? imgVertical : 
                          (headerImage && headerImage !== 'null') ? headerImage : '';

        gamesMap.set(appId, {
          appId,
          name: gameName,
          tags,
          headerImage: finalImage,
          imgVertical: finalImage,
          price: game.price || null,
          storeUrl: game.storeUrl || `https://store.steampowered.com/app/${appId}`,
          hours: 0,
          owners: 1, // Juego único = 1 copia
          ownersList: [{
            steamId: user.steamId,
            avatar: user.avatar,
            personaName: user.personaName
          }],
          hasAdultContent: this.isAdultContent(gameName, tags)
        });
      });
    });

    const allGames = Array.from(gamesMap.values());
    console.log('Total games built:', allGames.length);
    return allGames;
  }

  /**
   * Retorna juegos ordenados y filtrados según configuración actual
   * También separa y cuenta juegos sin imagen
   */
  protected getSortedFilteredGames(): Game[] {
    let games = this.getAllGames();

    // Aplicar filtro por nombre de juego
    const nameFilterValue = this.nameFilter().trim().toLowerCase();
    if (nameFilterValue) {
      games = games.filter(game => 
        game.name.toLowerCase().includes(nameFilterValue)
      );
    }

    // Aplicar filtro de tags - soporta múltiples tags separadas por comas
    const tagFilterValue = this.tagFilter().trim().toLowerCase();
    const selectedTags = this.selectedCommonTags();
    
    if (tagFilterValue || selectedTags.size > 0) {
      // Combinar tags personalizadas y tags comunes seleccionadas
      const searchTags: string[] = [];
      
      if (tagFilterValue) {
        searchTags.push(...tagFilterValue.split(',').map(t => t.trim()).filter(t => t.length > 0));
      }
      
      if (selectedTags.size > 0) {
        searchTags.push(...Array.from(selectedTags));
      }
      
      if (searchTags.length > 0) {
        games = games.filter(game => 
          // El juego debe tener TODAS las tags buscadas (AND)
          searchTags.every(searchTag => 
            game.tags.some(gameTag => gameTag.toLowerCase().includes(searchTag.toLowerCase()))
          )
        );
      }
    }

    // Aplicar filtro por usuarios seleccionados (AND - intersección)
    const selectedUsers = this.selectedUserFilters();
    if (selectedUsers.size > 0) {
      games = games.filter(game => {
        // El juego debe ser propiedad de TODOS los usuarios seleccionados
        if (!game.ownersList || game.ownersList.length === 0) return false;
        return Array.from(selectedUsers).every(userId => 
          game.ownersList!.some(owner => owner.steamId === userId)
        );
      });
    }

    // FILTRO ATLAS: Ocultar juegos +18 si Atlas NO está activo
    if (!this.atlasFilterActive()) {
      games = games.filter(game => !game.hasAdultContent);
    }

    // FILTRO CRÍTICO: Solo mostrar juegos que tienen headerImage o imgVertical
    // Esto excluye automáticamente:
    // 1. Juegos que fallaron appdetails (no tienen headerImage)
    // 2. Juegos que no están en la tienda de Steam
    // 3. Juegos que no fueron enriquecidos aún pero no están en DB
    // El imgIconUrl (de GetOwnedGames) NO es suficiente para mostrar el juego
    const gamesBeforeImageFilter = games.length;
    const gamesWithImage = games.filter(game => {
      const hasVerticalImage = game.headerImage || game.imgVertical;
      const hasImage = hasVerticalImage && hasVerticalImage.trim().length > 0;
      
      // Debug: Log de juegos sin imagen
      if (!hasImage && game.imgIconUrl) {
        console.log(`FILTRADO - Juego sin headerImage/imgVertical:`, {
          appId: game.appId,
          name: game.name,
          headerImage: game.headerImage,
          imgVertical: game.imgVertical,
          imgIconUrl: game.imgIconUrl,
          appdetailsFailed: (game as any)['appdetails-failed'] || (game as any).appdetailsFailed
        });
      }
      
      return hasImage;
    });
    const hiddenByImageCount = gamesBeforeImageFilter - gamesWithImage.length;
    
    // Actualizar contador de juegos ocultos (sin imagen vertical válida)
    this.hiddenGamesCount.set(hiddenByImageCount);

    // Aplicar ordenamiento solo a juegos con imagen
    const mode = this.sortMode();
    if (mode === 'alpha-asc') {
      gamesWithImage.sort((a, b) => a.name.localeCompare(b.name));
    } else if (mode === 'alpha-desc') {
      gamesWithImage.sort((a, b) => b.name.localeCompare(a.name));
    } else if (mode === 'copies-desc') {
      gamesWithImage.sort((a, b) => (b.owners || 0) - (a.owners || 0));
    } else if (mode === 'copies-asc') {
      gamesWithImage.sort((a, b) => (a.owners || 0) - (b.owners || 0));
    }

    return gamesWithImage;
  }

  /**
   * Agrupa juegos por cantidad de copias (solo cuando sort = copies-*)
   */
  protected getGamesByCopies(): Array<{ copies: number; games: Game[] }> {
    if (!this.sortMode().startsWith('copies-')) return [];

    const games = this.getSortedFilteredGames();
    const grouped = new Map<number, Game[]>();

    games.forEach(game => {
      const copies = game.owners || 1;
      if (!grouped.has(copies)) {
        grouped.set(copies, []);
      }
      grouped.get(copies)!.push(game);
    });

    const mode = this.sortMode();
    const result = Array.from(grouped.entries())
      .map(([copies, games]) => ({ copies, games }))
      .sort((a, b) => {
        // Ordenar grupos según el modo seleccionado
        if (mode === 'copies-desc') {
          return b.copies - a.copies; // Más copias primero
        } else {
          return a.copies - b.copies; // Menos copias primero
        }
      });

    return result;
  }

  protected setSortMode(mode: SortMode) {
    this.sortMode.set(mode);
  }

  protected toggleTagFilter() {
    this.showTagFilterInput.update(v => !v);
    if (!this.showTagFilterInput()) {
      this.tagFilter.set('');
    }
  }

  protected toggleTagDropdown() {
    this.showTagDropdown.update(v => !v);
    
    // Si se abre el dropdown, agregar listener para cerrar al hacer click fuera
    if (!this.showTagDropdown()) {
      this.removeClickOutsideListener();
    } else {
      setTimeout(() => this.addClickOutsideListener(), 0);
    }
  }
  
  private clickOutsideHandler = (event: MouseEvent) => {
    const dropdown = document.querySelector('.dropdown-content');
    const button = document.querySelector('.btn-filter');
    
    if (dropdown && button && 
        !dropdown.contains(event.target as Node) && 
        !button.contains(event.target as Node)) {
      this.showTagDropdown.set(false);
      this.removeClickOutsideListener();
    }
  };
  
  private addClickOutsideListener() {
    document.addEventListener('click', this.clickOutsideHandler);
  }
  
  private removeClickOutsideListener() {
    document.removeEventListener('click', this.clickOutsideHandler);
  }

  protected toggleCommonTag(tag: string) {
    this.selectedCommonTags.update(tags => {
      const newTags = new Set(tags);
      if (newTags.has(tag)) {
        newTags.delete(tag);
      } else {
        newTags.add(tag);
      }
      return newTags;
    });
  }

  protected clearCommonTags() {
    this.selectedCommonTags.set(new Set());
  }

  protected toggleUserFilter(steamId: string) {
    this.selectedUserFilters.update(filters => {
      const newFilters = new Set(filters);
      if (newFilters.has(steamId)) {
        newFilters.delete(steamId);
      } else {
        newFilters.add(steamId);
      }
      return newFilters;
    });
  }

  protected clearUserFilters() {
    this.selectedUserFilters.set(new Set());
  }

  protected isUserFilterSelected(steamId: string): boolean {
    return this.selectedUserFilters().has(steamId);
  }

  protected isTagSelected(tag: string): boolean {
    return this.selectedCommonTags().has(tag);
  }

  /**
   * Detecta si se escribió "atlas" en el filtro de tags y activa el filtro NSFW
   */
  protected checkAtlasFilter() {
    const filterValue = this.tagFilter().toLowerCase().trim();
    
    // Si escribió "atlas" exactamente y no está activo, pedir confirmación
    if (filterValue === 'atlas' && !this.atlasFilterActive()) {
      const confirmed = confirm(
        '⚠️ ADVERTENCIA - FILTRO ATLAS\n\n' +
        'Este filtro mostrará contenido NSFW y +18.\n\n' +
        'El contenido adulto incluye:\n' +
        '• Juegos con tags: Hentai, NSFW\n' +
        '• Juegos con contenido sexual explícito\n' +
        '• Títulos con palabras clave adultas\n\n' +
        '¿Está seguro de que desea activarlo?'
      );
      
      if (confirmed) {
        this.atlasFilterActive.set(true);
        this.tagFilter.set(''); // Limpiar el input
        console.log('🔞 Filtro Atlas activado - Contenido NSFW visible');
      } else {
        this.tagFilter.set(''); // Limpiar el input si cancela
        console.log('🔒 Filtro Atlas cancelado');
      }
    }
  }

  /**
   * Desactiva el filtro Atlas manualmente
   */
  protected deactivateAtlas() {
    if (this.atlasFilterActive()) {
      this.atlasFilterActive.set(false);
      console.log('🔒 Filtro Atlas desactivado');
    }
  }

  protected get commonTagOptions(): string[] {
    return ['Co-op', 'Online Co-Op', 'Multiplayer', 'Free to Play', 'Survival', 'Singleplayer', 'Action', 'Adventure', 'RPG', 'Strategy'];
  }

  protected toggleList(key: string) {
    this.expandedLists.update(obj => {
      const copy = { ...obj };
      copy[key] = !copy[key];
      return copy;
    });
  }

  protected isExpanded(key: string): boolean {
    const m = this.expandedLists();
    // Por defecto expandir la lista de comunes, colapsar las demás
    if (m[key] === undefined) {
      return key === 'common';
    }
    return !!m[key];
  }

  private announceChange(message: string) {
    // For screen readers only - completely hidden from visual display
    const announcement = document.createElement('div');
    announcement.setAttribute('role', 'status');
    announcement.setAttribute('aria-live', 'polite');
    announcement.style.position = 'absolute';
    announcement.style.left = '-10000px';
    announcement.style.width = '1px';
    announcement.style.height = '1px';
    announcement.style.overflow = 'hidden';
    announcement.textContent = message;
    document.body.appendChild(announcement);
    setTimeout(() => document.body.removeChild(announcement), 1000);
  }

  protected get comparisonStats() {
    const comp = this.comparison();
    if (!comp || !comp.estadisticas) return null;
    return comp.estadisticas;
  }

  protected getGameTags(game: any): string[] {
    if (!game.tags || !Array.isArray(game.tags)) return [];
    return game.tags
      .slice(0, 3)
      .map((t: any) => typeof t === 'string' ? t : t.tag || '')
      .filter(Boolean);
  }

  protected getUserUniqueGames(steamId: string): any[] {
    const comp = this.comparison();
    if (!comp || !comp.juegosUnicosPorUsuario) return [];
    return comp.juegosUnicosPorUsuario[steamId] || [];
  }

  protected hasUniqueGames(): boolean {
    const comp = this.comparison();
    if (!comp || !comp.juegosUnicosPorUsuario) return false;
    return Object.values(comp.juegosUnicosPorUsuario).some(games => games && games.length > 0);
  }

  /**
   * Construye listas ordenadas para mostrar: primero juegos comunes, luego los únicos de cada usuario
   */
  protected getComparisonLists(): Array<{ key: string; title: string; games: Game[] }> {
    const comp = this.comparison();
    if (!comp) return [];

    const lists: Array<{ key: string; title: string; games: Game[] }> = [];
    const currentUsers = this.users();

    // Helper para aplicar filtro y orden a una lista de juegos
    const applyFilterAndSort = (games: Game[]) => {
      let g = games.slice();
      
      // Filtro por nombre
      const nameFilterValue = this.nameFilter().trim().toLowerCase();
      if (nameFilterValue) {
        g = g.filter(game => game.name.toLowerCase().includes(nameFilterValue));
      }
      
      // Filtro por tags
      const tagFilterValue = this.tagFilter().trim().toLowerCase();
      const selectedTags = this.selectedCommonTags();
      
      if (tagFilterValue || selectedTags.size > 0) {
        const searchTags: string[] = [];
        
        if (tagFilterValue) {
          searchTags.push(...tagFilterValue.split(',').map(t => t.trim()).filter(t => t.length > 0));
        }
        
        if (selectedTags.size > 0) {
          searchTags.push(...Array.from(selectedTags));
        }
        
        if (searchTags.length > 0) {
          g = g.filter(game => 
            // El juego debe tener TODAS las tags buscadas (AND)
            searchTags.every(searchTag => 
              (game.tags || []).some(gameTag => gameTag.toLowerCase().includes(searchTag.toLowerCase()))
            )
          );
        }
      }

      // Filtro por usuarios seleccionados (AND - intersección)
      const selectedUsers = this.selectedUserFilters();
      if (selectedUsers.size > 0) {
        g = g.filter(game => {
          if (!game.ownersList || game.ownersList.length === 0) return false;
          return Array.from(selectedUsers).every(userId => 
            game.ownersList!.some(owner => owner.steamId === userId)
          );
        });
      }

      // FILTRO ATLAS: Ocultar juegos +18 si Atlas NO está activo
      if (!this.atlasFilterActive()) {
        g = g.filter(game => !game.hasAdultContent);
      }

      const mode = this.sortMode();
      if (mode === 'alpha-asc') {
        g.sort((a, b) => a.name.localeCompare(b.name));
      } else if (mode === 'alpha-desc') {
        g.sort((a, b) => b.name.localeCompare(a.name));
      } else if (mode === 'copies-desc') {
        g.sort((a, b) => (b.owners || 0) - (a.owners || 0));
      } else if (mode === 'copies-asc') {
        g.sort((a, b) => (a.owners || 0) - (b.owners || 0));
      }

      return g;
    };

    // Mapear juegos comunes
    if (Array.isArray(comp.juegosComunes) && comp.juegosComunes.length > 0) {
      const commonGames: Game[] = (comp.juegosComunes || []).map(j => {
        // mapJuegoToGame se encuentra en models
        try {
          // Import dinámico local: mapJuegoToGame disponible en este archivo import context
          // Pero para evitar dependencias circulares usamos la misma conversión rápida
          const tags = Array.isArray(j.tags)
            ? j.tags.map(t => typeof t === 'string' ? t : (t as any).tag || '').filter(Boolean)
            : [];

          const owners = j.cantidadCopias || 0;

          // Encontrar qué usuarios tienen este juego basándose en tiempoJugadoPorUsuario
          const playTimeMap = j.tiempoJugadoPorUsuario || {};
          const ownersList = currentUsers
            .filter(u => {
              // El backend puede usar personaName o steamId como key
              return playTimeMap[u.personaName] !== undefined || playTimeMap[u.steamId] !== undefined;
            })
            .map(u => ({
              steamId: u.steamId,
              avatar: u.avatar,
              personaName: u.personaName
            }));

          // Extraer las diferentes imágenes disponibles
          const imgVertical = (j as any)['img-vertical'] || (j as any).imgVertical || '';
          const headerImage = j.headerImage || '';
          // NO usar iconUrl como fallback - solo mostrar juegos con imágenes verticales válidas

          const gameName = j.name || 'Unknown';
          
          // Debug: Log todos los juegos para ver qué tienen
          if (j.appId === 280440 || !imgVertical) {
            console.log('🎮 Juego común (ID ' + j.appId + '):', {
              name: gameName,
              imgVertical: imgVertical,
              headerImage: headerImage,
              imgIconUrl: j.imgIconUrl,
              'img-vertical-backend': (j as any)['img-vertical'],
              appdetailsFailed: (j as any)['appdetails-failed']
            });
          }

          return {
            appId: j.appId || 0,
            name: gameName,
            tags,
            headerImage: headerImage,
            imgVertical: imgVertical || headerImage, // Solo vertical o header, SIN iconUrl
            price: j.price || null,
            storeUrl: j.storeUrl || `https://store.steampowered.com/app/${j.appId}`,
            hours: 0,
            owners,
            ownersList: ownersList,
            hasAdultContent: this.isAdultContent(gameName, tags)
          } as Game;
        } catch (e) {
          // Extraer las diferentes imágenes disponibles
          const imgVertical = (j as any)['img-vertical'] || (j as any).imgVertical || '';
          const headerImage = j.headerImage || '';
          // NO usar iconUrl como fallback - solo mostrar juegos con imágenes verticales válidas

          const gameName = j.name || 'Unknown';

          return {
            appId: j.appId || 0,
            name: gameName,
            tags: [],
            headerImage: headerImage,
            imgVertical: imgVertical || headerImage, // Solo vertical o header, SIN iconUrl
            price: j.price || null,
            storeUrl: j.storeUrl || `https://store.steampowered.com/app/${j.appId}`,
            hours: 0,
            owners: j.cantidadCopias || 0,
            ownersList: [],
            hasAdultContent: this.isAdultContent(gameName, [])
          } as Game;
        }
      });

      lists.push({ key: 'common', title: 'Juegos comunes', games: applyFilterAndSort(commonGames) });
    }

    // Construir mapa de claves que usa el backend (personaName || steamId)
    const keyToUsuario: Record<string, any> = {};
    if (Array.isArray(comp.usuarios)) {
      comp.usuarios.forEach((ud: any) => {
        const k = ud.personaName || ud.steamId || '';
        if (k) keyToUsuario[k] = ud;
      });
    }

    // Para cada usuario en el orden actual, añadir lista de únicos
    currentUsers.forEach(u => {
      // Encontrar la clave real usada en juegosUnicosPorUsuario para este usuario
      let foundKey: string | undefined;
      if (comp.juegosUnicosPorUsuario) {
        // 1) intentar coincidencia directa por personaName o steamId
        if (u.personaName && comp.juegosUnicosPorUsuario[u.personaName]) foundKey = u.personaName;
        else if (u.steamId && comp.juegosUnicosPorUsuario[u.steamId]) foundKey = u.steamId;

        // 2) intentar mapear a través de usuarios devueltos por el backend
        if (!foundKey && Object.keys(keyToUsuario).length > 0) {
          for (const k of Object.keys(comp.juegosUnicosPorUsuario)) {
            const ud = keyToUsuario[k];
            if (!ud) continue;
            if (ud.steamId === u.steamId || ud.personaName === u.personaName) {
              foundKey = k;
              break;
            }
          }
        }

        // 3) fallback: buscar una clave que contenga el steamId o personaName (tolerancia)
        if (!foundKey) {
          for (const k of Object.keys(comp.juegosUnicosPorUsuario)) {
            if (u.steamId && k.includes(u.steamId)) { foundKey = k; break; }
            if (u.personaName && k.includes(u.personaName)) { foundKey = k; break; }
          }
        }
      }

      const raw = (foundKey && comp.juegosUnicosPorUsuario ? comp.juegosUnicosPorUsuario[foundKey] : null) || [];
      const uniqueGames: Game[] = (raw || []).map(j => {
        const tags = Array.isArray(j.tags)
          ? j.tags.map(t => typeof t === 'string' ? t : (t as any).tag || '').filter(Boolean)
          : [];

        // Extraer las diferentes imágenes disponibles
        const imgVertical = (j as any)['img-vertical'] || (j as any).imgVertical || '';
        const headerImage = j.headerImage || '';
        // NO usar iconUrl como fallback - solo mostrar juegos con imágenes verticales válidas

        const gameName = j.name || 'Unknown';

        return {
          appId: j.appId || 0,
          name: gameName,
          tags,
          headerImage: headerImage,
          imgVertical: imgVertical || headerImage, // Solo vertical o header, SIN iconUrl
          price: j.price || null,
          storeUrl: j.storeUrl || `https://store.steampowered.com/app/${j.appId}`,
          hours: (j.tiempoJugadoPorUsuario && (j.tiempoJugadoPorUsuario[u.personaName] || j.tiempoJugadoPorUsuario[u.steamId])) || 0,
          owners: 1,
          ownersList: [{ steamId: u.steamId, avatar: u.avatar, personaName: u.personaName }],
          hasAdultContent: this.isAdultContent(gameName, tags)
        } as Game;
      });

      lists.push({ key: `unique-${u.steamId}`, title: `Únicos de ${u.personaName}`, games: applyFilterAndSort(uniqueGames) });
    });

    // Los juegos +18 ya están filtrados en applyFilterAndSort() según el estado de Atlas
    return lists;
  }
}
