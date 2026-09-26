import { Component, OnInit, signal, OnDestroy, ViewChild, ElementRef, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TooltipOverlayDirective } from '../../core/directives/tooltip-overlay.directive';
import { ActivatedRoute, Router } from '@angular/router';
import { BibliotecaUsuarioService } from '../../core/services/biblioteca-usuario.service';
import { BffService } from '../../core/services/bff.service';
import { ContentFilterService } from '../../core/services/content-filter.service';
import { UiBlockService } from '../../core/services/ui-block.service';
import { IBibliotecaUsuario, IJuegoUsuario, IGameAchievements } from '../../core/models/biblioteca-usuario.model';
import { Subscription, interval } from 'rxjs';

@Component({
  selector: 'app-biblioteca-usuario',
  standalone: true,
  imports: [CommonModule, FormsModule, TooltipOverlayDirective],
  templateUrl: './biblioteca-usuario.component.html',
  styleUrls: ['./biblioteca-usuario.component.css']
})
export class BibliotecaUsuarioComponent implements OnInit, OnDestroy {
  steamId = signal<string>('');
  biblioteca = signal<IBibliotecaUsuario | null>(null);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  refreshingGames = signal<Set<number>>(new Set());
  searchText = signal<string>('');
  selectedTags = signal<Set<string>>(new Set());
  availableTags = signal<string[]>([]);
  filtersOpen = signal<boolean>(false);
  tagSearchText = signal<string>('');
  // Filtro Atlas (NSFW)
  atlasFilterActive = signal<boolean>(false);
  // Nuevo: ordenamiento (A-Z, Z-A, horas jugadas)
  sortMode = signal<'alpha-asc' | 'alpha-desc' | 'hours-desc' | 'hours-asc' | 'hours2w-desc' | 'hours2w-asc'>('hours-desc');
  // Vista: grilla (header) o lista (tipo Steam)
  viewMode = signal<'grid' | 'list'>('grid');
  
  // Achievements
  expandedAchievements = signal<Set<number>>(new Set()); // appIds con achievements expandidos
  loadingAchievements = signal<Set<number>>(new Set()); // appIds cargando achievements
  achievementsData = signal<Map<number, IGameAchievements>>(new Map()); // Cache de achievements por appId
  achievementsSubscriptions = new Map<number, Subscription>(); // Subscriptions activas por appId
  achievementsAutoRefreshSubscription?: Subscription; // Timer para auto-refresh de achievements expandidos

  // Referencia al botón de filtros para posicionar el panel
  @ViewChild('filtersButton', { read: ElementRef }) filtersButton?: ElementRef;

  // Sincronización de biblioteca
  syncingLibrary = signal<boolean>(false);
  syncProgress = signal<string>('');
  showSyncComplete = signal<boolean>(false); // Mostrar mensaje de completado hasta cerrar manualmente
  lastRefreshDate = signal<string | null>(null);
  
  // Refresh completo de biblioteca
  refreshingAllGames = signal<boolean>(false);
  refreshAllProgress = signal<string>('');
  showRefreshAllComplete = signal<boolean>(false); // Mostrar mensaje de completado hasta cerrar manualmente
  
  // Control de bloqueo de UI durante operaciones largas
  isOperationInProgress = signal<boolean>(false);

  inputSteamId: string = '';
  
  private uiBlock = inject(UiBlockService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bibliotecaService: BibliotecaUsuarioService,
    private bffService: BffService,
    private contentFilter: ContentFilterService
  ) {}

  ngOnInit(): void {
    // Intentar obtener steamId de la ruta
    this.route.paramMap.subscribe(params => {
      const steamIdFromRoute = params.get('steamId');
      if (steamIdFromRoute) {
        this.steamId.set(steamIdFromRoute);
        this.inputSteamId = steamIdFromRoute;
        this.loadBiblioteca();
      }
    });

    // Auto-refresh de achievements cada 30 segundos
    this.achievementsAutoRefreshSubscription = interval(30000).subscribe(() => {
      this.autoRefreshExpandedAchievements();
    });
  }

  ngOnDestroy(): void {
    // Cancelar todas las subscriptions activas
    this.achievementsSubscriptions.forEach(sub => sub.unsubscribe());
    this.achievementsSubscriptions.clear();
    
    // Cancelar auto-refresh timer
    if (this.achievementsAutoRefreshSubscription) {
      this.achievementsAutoRefreshSubscription.unsubscribe();
    }
  }

  /**
   * Cierra el panel de filtros al hacer clic fuera de él
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.filtersOpen()) {
      const target = event.target as HTMLElement;
      const filtersPanel = document.querySelector('.filters-panel');
      const filtersButton = this.filtersButton?.nativeElement;
      
      // Si el clic no fue en el botón ni en el panel, cerrar
      if (filtersButton && !filtersButton.contains(target) && 
          filtersPanel && !filtersPanel.contains(target)) {
        this.closeFilters();
      }
    }
  }

  /**
   * Previene que el usuario abandone la página durante operaciones largas
   */
  @HostListener('window:beforeunload', ['$event'])
  unloadNotification($event: any): void {
    if (this.isOperationInProgress()) {
      $event.returnValue = '¿Estás seguro? Hay una operación en progreso que se perderá si sales de la página.';
    }
  }

  /**
   * Genera el texto del tooltip para un logro
   */
  getAchievementTooltip(displayName: string, description?: string): string {
    return displayName + (description ? '\n' + description : '');
  }

  /**
   * Carga la biblioteca del usuario
   */
  loadBiblioteca(): void {
    const id = this.steamId();
    if (!id || id.trim() === '') {
      this.error.set('Por favor ingresa un Steam ID válido');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.biblioteca.set(null);

    this.bibliotecaService.getBibliotecaUsuario(id, false).subscribe({
      next: (data) => {
        this.biblioteca.set(data);
        // Recalcular tags disponibles
        this.rebuildAvailableTags();
        // Cargar última fecha de sincronización
        this.loadLastRefreshDate();
        this.loading.set(false);
        console.log('Biblioteca cargada:', data);
      },
      error: (err) => {
        console.error('Error al cargar biblioteca:', err);
        this.error.set(err.message || 'Error al cargar la biblioteca del usuario');
        this.loading.set(false);
      }
    });
  }

  /**
   * Reconstruye la lista de tags disponibles a partir de los juegos cargados
   */
  private rebuildAvailableTags(): void {
    const b = this.biblioteca();
    const set = new Set<string>();
    if (b?.juegos) {
      for (const j of b.juegos) {
        if (j.tags && Array.isArray(j.tags)) {
          for (const t of j.tags) {
            const tag = (t || '').trim();
            if (tag) set.add(tag);
          }
        }
      }
    }
    this.availableTags.set(Array.from(set).sort((a,b) => a.localeCompare(b)));
  }

  /** Detecta si un juego es +18 usando el filtro Atlas compartido */
  private isAdultContent(name: string, tags: string[] = []): boolean {
    return this.contentFilter.isAdultContent(name, tags);
  }

  /** Cambia selección de un tag */
  toggleTag(tag: string): void {
    const curr = new Set(this.selectedTags());
    if (curr.has(tag)) curr.delete(tag); else curr.add(tag);
    this.selectedTags.set(curr);
  }

  /** Limpia todos los filtros */
  clearFilters(): void {
    this.searchText.set('');
    this.selectedTags.set(new Set());
    this.tagSearchText.set('');
    this.atlasFilterActive.set(false);
  }

  /** Juegos filtrados por texto, tags y ordenados según configuración */
  filteredJuegos(): IJuegoUsuario[] {
    const b = this.biblioteca();
    if (!b?.juegos) return [];
    const text = this.searchText().toLowerCase().trim();
    const tags = this.selectedTags();

    let list = b.juegos.filter(j => {
      // Filtro por nombre
      const matchesText = !text || (j.name?.toLowerCase().includes(text));
      if (!matchesText) return false;

      // Filtrar juegos sin headerImage
      if (!j.headerImage || j.headerImage.trim() === '') return false;

      const isAdult = this.isAdultContent(j.name || '', (j.tags as string[]) || []);

      // Filtro por tags seleccionadas (checkboxes) - requiere TODAS (AND)
      if (tags.size && !(this.atlasFilterActive() && isAdult)) {
        const jt = new Set((j.tags || []).map(t => (t || '').trim()));
        for (const t of tags) {
          if (!jt.has(t)) return false;
        }
      }

      // Filtro ATLAS: si NO está activo, ocultar +18
      if (!this.atlasFilterActive() && isAdult) return false;

      return true;
    });

    // Ordenamiento
    const mode = this.sortMode();
    if (mode === 'alpha-asc') {
      list.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
    } else if (mode === 'alpha-desc') {
      list.sort((a, b) => (b.name || '').localeCompare(a.name || ''));
    } else if (mode === 'hours-desc') {
      list.sort((a, b) => (b.playtimeForever || 0) - (a.playtimeForever || 0));
    } else if (mode === 'hours-asc') {
      list.sort((a, b) => (a.playtimeForever || 0) - (b.playtimeForever || 0));
    } else if (mode === 'hours2w-desc') {
      list.sort((a, b) => (b.playtime2Weeks || 0) - (a.playtime2Weeks || 0));
    } else if (mode === 'hours2w-asc') {
      list.sort((a, b) => (a.playtime2Weeks || 0) - (b.playtime2Weeks || 0));
    }

    return list;
  }

  /** Tags disponibles filtradas por el texto de búsqueda de tags */
  filteredAvailableTags(): string[] {
    const txt = this.tagSearchText().toLowerCase().trim();
    const all = this.availableTags();
    let result = !txt ? all : all.filter(t => t.toLowerCase().includes(txt));
    // Mostrar opción especial "Atlas" solo cuando el usuario escribe su nombre en el buscador
    if (txt.includes('atlas')) {
      // Evitar duplicado por si existe una tag real llamada Atlas
      const hasAtlas = result.some(t => t.toLowerCase() === 'atlas');
      if (!hasAtlas) {
        result = ['Atlas', ...result];
      }
    }
    return result;
  }

  /** Alterna el panel de filtros */
  toggleFilters(): void {
    this.filtersOpen.set(!this.filtersOpen());
  }

  /** Cierra el panel de filtros */
  closeFilters(): void {
    this.filtersOpen.set(false);
  }

  /** Activa manualmente el filtro Atlas previa confirmación */
  activateAtlas(): void {
    if (this.atlasFilterActive()) return;
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
      console.log('🔞 Filtro Atlas activado - Contenido NSFW visible');
    }
  }

  /** Desactiva manualmente el filtro Atlas */
  deactivateAtlas(): void {
    if (this.atlasFilterActive()) {
      this.atlasFilterActive.set(false);
      console.log('� Filtro Atlas desactivado');
    }
  }

  /** Alterna el checkbox especial de Atlas en la lista de tags */
  toggleAtlasCheckbox(checked: boolean): void {
    if (checked) {
      this.activateAtlas();
    } else {
      this.deactivateAtlas();
    }
  }

  /**
   * Busca la biblioteca por Steam ID
   */
  buscarBiblioteca(): void {
    if (this.inputSteamId && this.inputSteamId.trim() !== '') {
      this.steamId.set(this.inputSteamId.trim());
      this.loadBiblioteca();
    } else {
      this.error.set('Por favor ingresa un Steam ID');
    }
  }

  /**
   * Actualiza la información de un juego específico
   */
  refreshJuego(juego: IJuegoUsuario): void {
    // No requerimos steamId para refrescar un juego: el BFF/Backend opera por appId
    // Asegurar que obtenemos un appId válido (algunas fuentes usan `appid`)
    const rawAppId = (juego as any).appId ?? (juego as any).appid;
    const appId = Number(rawAppId);
    if (!appId) {
      console.warn('refreshJuego: appId inválido para juego', juego);
      alert(`No se encontró AppId válido para ${juego.name}`);
      return;
    }

    // Evitar llamar appdetails para juegos de servidor (test/public/dedicated)
    const nameLower = (juego.name || '').toLowerCase();
    if (nameLower.includes('test server') || nameLower.includes('public server') || nameLower.includes('dedicated server')) {
      console.warn('refreshJuego: ignorando juego de servidor:', juego.name);
      return;
    }

    // Marcar el juego como en proceso de refresh
    const refreshing = new Set(this.refreshingGames());
    refreshing.add(appId);
    this.refreshingGames.set(refreshing);

    // Obtener steamId para ingesta de logros
    const steamId = this.steamId();
    
    // IMPORTANTE: Limpiar caché de logros ANTES de actualizar el juego
    // para forzar recarga desde Steam API cuando se vuelvan a solicitar
    const wasExpanded = this.expandedAchievements().has(appId);
    if (wasExpanded) {
      const cache = new Map(this.achievementsData());
      cache.delete(appId);
      this.achievementsData.set(cache);
      console.log('Caché de logros limpiado para AppID:', appId);
    }
    
    // Nuevo flujo: usar BFF para upsert global del juego (con steamId para logros)
    this.bffService.getBibliotecaJuego(appId, steamId).subscribe({
      next: (payload: any) => {
        console.log('Juego actualizado (BFF):', payload);

        // Mapear respuesta a nuestro modelo de biblioteca de usuario
        const biblio = this.biblioteca();
        if (biblio) {
          const juegoIndex = biblio.juegos.findIndex(j => (j as any).appId === appId || (j as any).appid === appId);
          if (juegoIndex !== -1) {
            const updated = { ...biblio.juegos[juegoIndex] } as IJuegoUsuario;
            updated.name = payload.name ?? updated.name;
            updated.headerImage = payload.headerImage ?? updated.headerImage;
            updated.price = (payload.price ?? undefined) as any;
            updated.isFree = (payload.isFree ?? undefined) as any;
            updated.storeUrl = payload.storeUrl ?? updated.storeUrl;
            updated.libraryImage = payload.imgVertical ?? updated.libraryImage;
            // actualiza tags si vinieron
            if ((payload as any).tags && Array.isArray((payload as any).tags)) {
              const list = ((payload as any).tags as Array<{tag?: string}>).map(t => (t?.tag || '').trim()).filter(Boolean);
              updated.tags = list;
            }
            // playtimeForever se mantiene desde el registro actual del usuario

            biblio.juegos[juegoIndex] = updated;
            this.biblioteca.set({ ...biblio });
            // Recalcular tags por si cambió
            this.rebuildAvailableTags();
          }
        }

        // SIEMPRE recargar logros forzando actualización desde Steam API
        // Esto garantiza que si el usuario obtuvo nuevos logros, se actualicen en BD
        // Llamamos al refresh independientemente de si los logros están expandidos
        console.log('🔄 REFRESH: Iniciando actualización de logros para AppID:', appId, 'Usuario:', steamId);
        
        // Validar que tengamos steamId antes de intentar refrescar logros
        if (!steamId || steamId.trim() === '') {
          console.warn('⚠️ No se puede refrescar logros: steamId no disponible');
          // Quitar de la lista de refreshing
          const refreshing = new Set(this.refreshingGames());
          refreshing.delete(appId);
          this.refreshingGames.set(refreshing);
          return;
        }
        
        this.bibliotecaService.refreshGameAchievements(steamId, appId).subscribe({
          next: (achievements) => {
            // Verificar si la operación fue exitosa
            if (achievements.success === false) {
              console.info('ℹ️ REFRESH: No se pudieron actualizar logros para AppID:', appId, '-', achievements.error);
              console.info('📝 Esto es normal si el juego no tiene logros o el perfil es privado');
              
              // Si los logros estaban expandidos, cerrar la sección
              if (wasExpanded) {
                const expanded = new Set(this.expandedAchievements());
                expanded.delete(appId);
                this.expandedAchievements.set(expanded);
              }
              
              // Quitar de la lista de refreshing
              const refreshing = new Set(this.refreshingGames());
              refreshing.delete(appId);
              this.refreshingGames.set(refreshing);
              return;
            }
            
            console.log('✅ REFRESH: Logros actualizados exitosamente en BD para AppID:', appId);
            console.log('📊 Total logros en respuesta del refresh:', achievements.totalAchievements, 'Desbloqueados:', achievements.unlockedAchievements);
            
            // IMPORTANTE: Esperar un momento para asegurar que la transacción de BD se complete
            // Luego recargar desde BD para obtener los datos actualizados
            console.log('⏳ Esperando 500ms para asegurar que la BD se actualice...');
            setTimeout(() => {
              console.log('🔄 Recargando logros desde BD para AppID:', appId);
              this.bibliotecaService.getGameAchievements(steamId, appId).subscribe({
                next: (freshAchievements) => {
                  console.log('✅ Logros recargados desde BD:', freshAchievements);
                  console.log('📊 Total logros desde BD:', freshAchievements.totalAchievements, 'Desbloqueados:', freshAchievements.unlockedAchievements);
                  
                  // CRÍTICO: Crear un NUEVO Map para que Angular detecte el cambio
                  const cache = new Map(this.achievementsData());
                  cache.set(appId, freshAchievements);
                  
                  // Forzar actualización del signal creando una nueva instancia
                  this.achievementsData.set(new Map(cache));
                  
                  console.log('🔄 Caché de logros actualizado en UI para AppID:', appId);
                  console.log('🎯 Nuevo estado del caché:', this.achievementsData().get(appId));
                  
                  // Si los logros estaban expandidos, la UI se actualiza automáticamente
                  // porque el template lee de achievementsData()
                },
                error: (dbErr) => {
                  console.error('❌ Error al recargar logros desde BD:', dbErr);
                  // Si falla la recarga desde BD, usar los datos del refresh como fallback
                  const cache = new Map(this.achievementsData());
                  cache.set(appId, achievements);
                  this.achievementsData.set(new Map(cache));
                }
              });
            }, 500);
            
            // Quitar de la lista de refreshing cuando termine
            const refreshing = new Set(this.refreshingGames());
            refreshing.delete(appId);
            this.refreshingGames.set(refreshing);
          },
          error: (err) => {
            console.error('❌ REFRESH: Error al actualizar logros para AppID:', appId, err);
            
            // NO mostrar mensaje de error al usuario durante refresh
            // El refresh del juego fue exitoso, solo falló la actualización de logros
            // que puede ser normal si el juego no tiene logros o el perfil es privado
            
            // Si los logros estaban expandidos, NO actualizar el caché con error
            // Simplemente mantener el estado anterior o cerrar la expansión
            if (wasExpanded) {
              // Opcional: cerrar la sección de logros expandida
              const expanded = new Set(this.expandedAchievements());
              expanded.delete(appId);
              this.expandedAchievements.set(expanded);
            }
            
            // Quitar de la lista de refreshing incluso si falla
            const refreshing = new Set(this.refreshingGames());
            refreshing.delete(appId);
            this.refreshingGames.set(refreshing);
          }
        });
      },
      error: (err: any) => {
        console.error('Error al actualizar juego (BFF):', err);
        alert(`Error al actualizar ${juego.name}: ${err.message || 'Error desconocido'}`);

        // Quitar de la lista de refreshing
        const refreshing = new Set(this.refreshingGames());
        refreshing.delete(appId);
        this.refreshingGames.set(refreshing);
      }
    });
  }

  /**
   * Verifica si un juego está siendo actualizado
   */
  isRefreshing(appId: number): boolean {
    return this.refreshingGames().has(appId);
  }

  /**
   * Formatea minutos a horas
   */
  formatPlaytime(minutes: number): string {
    if (minutes === 0) return '0 horas';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours === 0) return `${mins} min`;
    if (mins === 0) return `${hours} hrs`;
    return `${hours} hrs ${mins} min`;
  }

  /**
   * Obtiene la URL de la imagen header del juego
   */
  getHeaderImage(juego: IJuegoUsuario): string {
    return juego.headerImage || `https://cdn.akamai.steamstatic.com/steam/apps/${juego.appId}/header.jpg`;
  }

  /**
   * Abre la página de Steam del juego
   */
  abrirEnSteam(appId: number): void {
    window.open(`https://store.steampowered.com/app/${appId}`, '_blank');
  }

  /**
   * Navega a la vista de juego aleatorio
   */
  irAJuegoAleatorio(): void {
    const steamId = this.steamId();
    if (steamId) {
      this.router.navigate(['/biblioteca', steamId, 'aleatorio']);
    }
  }

  /**
   * Alterna la visualización de logros para un juego
   */
  toggleAchievements(juego: IJuegoUsuario): void {
    const appId = juego.appId;
    const expanded = new Set(this.expandedAchievements());
    
    if (expanded.has(appId)) {
      // Ya está expandido, colapsar
      expanded.delete(appId);
      this.expandedAchievements.set(expanded);
      
      // Cancelar la petición HTTP si está en curso
      const subscription = this.achievementsSubscriptions.get(appId);
      if (subscription) {
        subscription.unsubscribe();
        this.achievementsSubscriptions.delete(appId);
        
        // Limpiar estado de loading
        const loading = new Set(this.loadingAchievements());
        loading.delete(appId);
        this.loadingAchievements.set(loading);
      }
    } else {
      // Expandir y cargar logros
      expanded.add(appId);
      this.expandedAchievements.set(expanded);
      
      const cached = this.achievementsData().get(appId);
      // Si no está en caché, cargar normalmente (intentará BD primero)
      // Si está en caché pero es viejo, el usuario puede usar el botón refresh
      if (!cached) {
        this.loadAchievements(appId, false);
      }
    }
  }

  /**
   * Carga los logros de un juego
   * Utiliza las siguientes APIs de Steam:
   * - GetPlayerAchievements: https://api.steampowered.com/ISteamUserStats/GetPlayerAchievements/v1/
   *   Obtiene el progreso del usuario (achieved: 0 o 1) para cada logro
   * - GetSchemaForGame: https://api.steampowered.com/ISteamUserStats/GetSchemaForGame/v2/
   *   Obtiene los metadatos de cada logro (nombre, descripción, iconos)
   * 
   * Los iconos se muestran en color si achieved=1, o en gris si achieved=0
   */
  private loadAchievements(appId: number, forceRefresh: boolean = false): void {
    const steamId = this.steamId();
    if (!steamId) return;

    const loading = new Set(this.loadingAchievements());
    loading.add(appId);
    this.loadingAchievements.set(loading);

    // Cancelar subscription previa si existe
    const existingSub = this.achievementsSubscriptions.get(appId);
    if (existingSub) {
      existingSub.unsubscribe();
    }

    // Usar el método de refresh si se fuerza, o el normal si no
    const achievementsObservable = forceRefresh
      ? this.bibliotecaService.refreshGameAchievements(steamId, appId)
      : this.bibliotecaService.getGameAchievements(steamId, appId);

    const subscription = achievementsObservable.subscribe({
      next: (data) => {
        const cache = new Map(this.achievementsData());
        cache.set(appId, data);
        this.achievementsData.set(cache);

        const loading = new Set(this.loadingAchievements());
        loading.delete(appId);
        this.loadingAchievements.set(loading);
        
        this.achievementsSubscriptions.delete(appId);
      },
      error: (err) => {
        console.error(`Error al cargar logros para ${appId}:`, err);
        
        // Extraer mensaje de error más descriptivo
        let errorMessage = 'No se pudieron obtener los logros';
        
        // Verificar si es un error de base de datos (tabla no existe)
        if (err.message && err.message.includes('relation') && err.message.includes('does not exist')) {
          errorMessage = 'Este juego no tiene logros disponibles o el sistema de logros no está configurado';
        } else if (err.error?.error) {
          // Usar el mensaje de error del backend si está disponible
          errorMessage = err.error.error;
        } else if (err.error?.message) {
          errorMessage = err.error.message;
        } else if (err.message) {
          errorMessage = err.message;
        } else if (err.status === 500) {
          errorMessage = 'Este juego no tiene logros disponibles';
        } else if (err.status === 404) {
          errorMessage = 'No existen logros para este juego';
        }
        
        // Guardar error en caché
        const cache = new Map(this.achievementsData());
        cache.set(appId, {
          steamId,
          appId,
          gameName: '',
          totalAchievements: 0,
          unlockedAchievements: 0,
          achievements: [],
          success: false,
          error: errorMessage
        });
        this.achievementsData.set(cache);

        const loading = new Set(this.loadingAchievements());
        loading.delete(appId);
        this.loadingAchievements.set(loading);
        
        this.achievementsSubscriptions.delete(appId);
      }
    });
    
    // Guardar subscription para poder cancelarla
    this.achievementsSubscriptions.set(appId, subscription);
  }

  /**
   * Verifica si los logros de un juego están expandidos
   */
  isAchievementsExpanded(appId: number): boolean {
    return this.expandedAchievements().has(appId);
  }

  /**
   * Verifica si los logros de un juego se están cargando
   */
  isLoadingAchievements(appId: number): boolean {
    return this.loadingAchievements().has(appId);
  }

  /**
   * Obtiene los logros de un juego desde el caché
   */
  getAchievements(appId: number): IGameAchievements | undefined {
    return this.achievementsData().get(appId);
  }

  /**
   * Refresca los logros de todos los juegos que están actualmente expandidos
   */
  private refreshExpandedAchievements(): void {
    const expanded = this.expandedAchievements();
    if (expanded.size === 0) return;

    console.log(`Refrescando logros de ${expanded.size} juego(s) expandido(s)...`);
    
    // Limpiar caché de los juegos expandidos
    const cache = new Map(this.achievementsData());
    expanded.forEach(appId => {
      cache.delete(appId);
    });
    this.achievementsData.set(cache);

    // Recargar logros de cada juego expandido con forceRefresh
    expanded.forEach(appId => {
      this.loadAchievements(appId, true); // true = forzar refresh desde Steam API
    });
  }

  /**
   * Auto-refresh silencioso de logros expandidos (sin limpiar caché)
   * Se ejecuta cada 30 segundos automáticamente
   */
  private autoRefreshExpandedAchievements(): void {
    const expanded = this.expandedAchievements();
    if (expanded.size === 0) return;

    const steamId = this.steamId();
    if (!steamId) return;

    // Recargar logros de cada juego expandido SIN limpiar caché
    // Esto permite actualización transparente sin parpadeo
    expanded.forEach(appId => {
      // Solo refrescar si no está ya cargando
      if (!this.loadingAchievements().has(appId)) {
        this.bibliotecaService.refreshGameAchievements(steamId, appId).subscribe({
          next: (data) => {
            const cache = new Map(this.achievementsData());
            const currentData = cache.get(appId);
            
            // Solo actualizar si cambió el número de logros desbloqueados
            if (!currentData || currentData.unlockedAchievements !== data.unlockedAchievements) {
              console.log(`🔄 Auto-refresh: Logros actualizados para AppID ${appId} (${data.unlockedAchievements}/${data.totalAchievements})`);
              cache.set(appId, data);
              this.achievementsData.set(cache);
            }
          },
          error: (err) => {
            // Silenciar errores del auto-refresh para no molestar al usuario
            console.debug(`Auto-refresh falló para AppID ${appId}:`, err.message);
          }
        });
      }
    });
  }

  /**
   * Reintenta la carga de logros para un juego
   */
  retryAchievements(appId: number): void {
    // Limpiar error del caché
    const cache = new Map(this.achievementsData());
    cache.delete(appId);
    this.achievementsData.set(cache);
    
    // Recargar logros
    this.loadAchievements(appId);
  }

  /**
   * Calcula el porcentaje de logros desbloqueados
   */
  getAchievementProgress(appId: number): number {
    const data = this.achievementsData().get(appId);
    if (!data || !data.totalAchievements || data.totalAchievements === 0) return 0;
    const unlocked = data.unlockedAchievements || 0;
    return Math.round((unlocked / data.totalAchievements) * 100);
  }

  /**
   * Sincroniza toda la biblioteca del usuario con la base de datos
   * Actualiza juegos que no tienen tags, detalles o imágenes completas
   * También actualiza los logros de todos los juegos expandidos
   */
  /**
   * Carga la última fecha de sincronización desde localStorage
   */
  private loadLastRefreshDate(): void {
    const id = this.steamId();
    if (!id) {
      this.lastRefreshDate.set(null);
      return;
    }
    
    const key = `lastRefresh_${id}`;
    const saved = localStorage.getItem(key);
    if (saved) {
      const timestamp = parseInt(saved, 10);
      if (!isNaN(timestamp)) {
        this.lastRefreshDate.set(this.formatLastRefresh(timestamp));
      }
    } else {
      this.lastRefreshDate.set(null);
    }
  }

  /**
   * Guarda la fecha de sincronización actual en localStorage
   */
  private saveLastRefreshDate(): void {
    const id = this.steamId();
    if (!id) return;
    
    const key = `lastRefresh_${id}`;
    const timestamp = Date.now();
    localStorage.setItem(key, timestamp.toString());
    this.lastRefreshDate.set(this.formatLastRefresh(timestamp));
  }

  /**
   * Formatea la fecha de sincronización de manera amigable
   */
  private formatLastRefresh(timestamp: number): string {
    const now = Date.now();
    const diff = now - timestamp;
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (seconds < 60) return 'Hace un momento';
    if (minutes < 60) return `Hace ${minutes} minuto${minutes !== 1 ? 's' : ''}`;
    if (hours < 24) return `Hace ${hours} hora${hours !== 1 ? 's' : ''}`;
    if (days < 7) return `Hace ${days} día${days !== 1 ? 's' : ''}`;
    
    // Para más de 7 días, mostrar la fecha
    const date = new Date(timestamp);
    return date.toLocaleDateString('es-ES', { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /**
   * Sincroniza toda la biblioteca del usuario con la base de datos
   * Actualiza juegos que no tienen tags, detalles o imágenes completas
   * También actualiza los logros de todos los juegos expandidos
   */
  sincronizarBiblioteca(): void {
    const id = this.steamId();
    if (!id || id.trim() === '') {
      return;
    }

    const juegosAntesDeSync = this.biblioteca()?.juegos?.length || 0;
    const startTime = Date.now();

    this.isOperationInProgress.set(true);
    this.uiBlock.block();
    this.syncingLibrary.set(true);
    this.showSyncComplete.set(false);
    this.syncProgress.set('🔄 Sincronizando biblioteca con Steam...');
    
    // Actualizar mensaje cada 10 segundos con tiempo transcurrido
    const progressInterval = setInterval(() => {
      if (!this.syncingLibrary()) {
        clearInterval(progressInterval);
        return;
      }
      const elapsed = Math.floor((Date.now() - startTime) / 1000);
      const minutes = Math.floor(elapsed / 60);
      const seconds = elapsed % 60;
      this.syncProgress.set(
        `🔄 Sincronizando biblioteca... (${minutes}m ${seconds}s transcurridos)`
      );
    }, 10000);

    this.bibliotecaService.syncBiblioteca(id).subscribe({
      next: (resultado) => {
        clearInterval(progressInterval);
        
        // Si hay un error crítico (total = 0 y errores > 0), mostrar error
        if (resultado.total === 0 && resultado.errores > 0) {
          this.syncProgress.set(`❌ ${resultado.mensaje}`);
          this.showSyncComplete.set(true);
          this.syncingLibrary.set(false);
          return;
        }
        
        // Recargar biblioteca primero para obtener el conteo actualizado
        this.bibliotecaService.getBibliotecaUsuario(id, true).subscribe({
          next: (bibliotecaActualizada: IBibliotecaUsuario) => {
            const juegosNuevos = (bibliotecaActualizada.juegos?.length || 0) - juegosAntesDeSync;
            
            let mensaje: string;
            
            // Si no se sincronizó ninguno, significa que todo está actualizado
            if (resultado.sincronizados === 0 && resultado.errores === 0) {
              mensaje = `✅ Biblioteca ya está sincronizada • ${resultado.total} juego${resultado.total > 1 ? 's' : ''} verificado${resultado.total > 1 ? 's' : ''} 🎮`;
            } else {
              mensaje = `✅ Sincronización completada: ${resultado.sincronizados} de ${resultado.total} juegos actualizados`;
              
              if (juegosNuevos > 0) {
                mensaje += ` • ${juegosNuevos} juego${juegosNuevos > 1 ? 's' : ''} nuevo${juegosNuevos > 1 ? 's' : ''} añadido${juegosNuevos > 1 ? 's' : ''} 🎮`;
              } else if (juegosNuevos < 0) {
                mensaje += ` • ${Math.abs(juegosNuevos)} juego${Math.abs(juegosNuevos) > 1 ? 's' : ''} eliminado${Math.abs(juegosNuevos) > 1 ? 's' : ''}`;
              }
              
              if (resultado.errores > 0) {
                mensaje += ` (${resultado.errores} error${resultado.errores > 1 ? 'es' : ''})`;
                
                // Si hay juegos con error, mostrarlos
                if (resultado.juegosConError && resultado.juegosConError.length > 0) {
                  console.warn('⚠️ Juegos que fallaron durante la sincronización:');
                  resultado.juegosConError.forEach(juego => console.warn('  -', juego));
                  
                  // Agregar hint en el mensaje
                  mensaje += ` • Ver consola para detalles`;
                }
              }
            }
            
            this.syncProgress.set(mensaje);
            this.showSyncComplete.set(true);
            this.syncingLibrary.set(false);
            this.isOperationInProgress.set(false);
            this.uiBlock.unblock();
            
            // Actualizar la biblioteca en el signal
            this.biblioteca.set(bibliotecaActualizada);
            
            // Guardar fecha de sincronización
            this.saveLastRefreshDate();
            
            // Refrescar logros de juegos expandidos
            this.refreshExpandedAchievements();
          },
          error: (errorRecarga) => {
            // Si falla la recarga, mostrar mensaje básico
            console.warn('No se pudo recargar biblioteca después de sync, mostrando resultado de sync:', errorRecarga);
            this.syncProgress.set(
              `✅ Sincronización completada: ${resultado.sincronizados} de ${resultado.total} juegos actualizados` +
              (resultado.errores > 0 ? ` (${resultado.errores} errores)` : '')
            );
            this.showSyncComplete.set(true);
            this.syncingLibrary.set(false);
            this.isOperationInProgress.set(false);
            this.uiBlock.unblock();
            this.saveLastRefreshDate();
            this.loadBiblioteca();
          }
        });
      },
      error: (err) => {
        clearInterval(progressInterval);
        console.error('Error al sincronizar biblioteca:', err);
        
        let errorMessage = 'No se pudo sincronizar la biblioteca';
        
        // Intentar extraer mensaje de error más específico
        if (err.error && typeof err.error === 'object') {
          if (err.error.mensaje) {
            errorMessage = err.error.mensaje;
          } else if (err.error.message) {
            errorMessage = err.error.message;
          }
        } else if (err.message) {
          errorMessage = err.message;
        }
        
        // Si es timeout
        if (err.status === 0 || err.statusText === 'Unknown Error') {
          errorMessage = 'Tiempo de espera agotado. La sincronización puede tardar varios minutos si tienes muchos juegos.';
        }
        
        this.syncProgress.set(`❌ Error: ${errorMessage}`);
        this.showSyncComplete.set(true);
        this.syncingLibrary.set(false);
        this.isOperationInProgress.set(false);
        this.uiBlock.unblock();
      }
    });
  }

  /**
   * Actualiza TODOS los juegos de la biblioteca
   * Llama a refresh para cada juego para actualizar precios, imágenes, tags, etc.
   * ADVERTENCIA: Puede tardar varias horas con bibliotecas grandes
   */
  refreshAllGames(): void {
    const id = this.steamId();
    if (!id || id.trim() === '') {
      return;
    }

    const biblioteca = this.biblioteca();
    if (!biblioteca || !biblioteca.juegos || biblioteca.juegos.length === 0) {
      alert('No hay juegos para actualizar');
      return;
    }

    const totalJuegos = biblioteca.juegos.length;
    const estimatedMinutes = Math.ceil((totalJuegos * 2) / 60); // 2 segundos por juego (wait entre llamadas)
    
    // Mostrar confirmación con advertencia
    const confirmed = confirm(
      `⚠️ ADVERTENCIA: Esta operación actualizará TODOS los ${totalJuegos} juegos de tu biblioteca.\n\n` +
      `⏱️ Tiempo estimado: ${estimatedMinutes} minutos (puede tardar varias horas)\n` +
      `📊 Se actualizarán precios, imágenes, tags y logros de cada juego\n` +
      `🔄 Se realizan 3 llamadas a Steam API por cada juego (con espera de 2s)\n\n` +
      `¿Estás seguro de que deseas continuar?`
    );
    
    if (!confirmed) {
      return;
    }

    const startTime = Date.now();
    this.isOperationInProgress.set(true);
    this.uiBlock.block();
    this.refreshingAllGames.set(true);
    this.showRefreshAllComplete.set(false);
    this.refreshAllProgress.set(`🔄 Iniciando actualización de ${totalJuegos} juegos...`);
    
    // Actualizar mensaje cada 10 segundos con progreso
    const progressInterval = setInterval(() => {
      if (!this.refreshingAllGames()) {
        clearInterval(progressInterval);
        return;
      }
      const elapsed = Math.floor((Date.now() - startTime) / 1000);
      const minutes = Math.floor(elapsed / 60);
      const seconds = elapsed % 60;
      // El mensaje se actualizará con el progreso del backend
    }, 10000);

    this.bibliotecaService.refreshAllGames(id).subscribe({
      next: (resultado) => {
        clearInterval(progressInterval);
        
        // Si hay un error crítico
        if (resultado.total === 0 && resultado.errores > 0) {
          this.refreshAllProgress.set(`❌ ${resultado.mensaje}`);
          this.showRefreshAllComplete.set(true);
          this.refreshingAllGames.set(false);
          return;
        }
        
        // Construir mensaje de resultado
        let mensaje: string;
        
        if (resultado.actualizados === 0 && resultado.errores === 0) {
          mensaje = `✅ Todos los juegos ya estaban actualizados • ${resultado.total} juego${resultado.total > 1 ? 's' : ''} verificado${resultado.total > 1 ? 's' : ''} 🎮`;
        } else {
          mensaje = `✅ Actualización completada: ${resultado.actualizados} de ${resultado.total} juegos actualizados`;
          
          if (resultado.errores > 0) {
            mensaje += ` (${resultado.errores} error${resultado.errores > 1 ? 'es' : ''})`;
            
            // Si hay juegos con error, mostrarlos
            if (resultado.juegosConError && resultado.juegosConError.length > 0) {
              console.warn('⚠️ Juegos que fallaron durante la actualización:');
              resultado.juegosConError.forEach(juego => console.warn('  -', juego));
              mensaje += ` • Ver consola para detalles`;
            }
          }
        }
        
        // Mostrar mensaje y marcarlo como persistente
        this.refreshAllProgress.set(mensaje);
        this.showRefreshAllComplete.set(true);
        this.refreshingAllGames.set(false);
        this.isOperationInProgress.set(false);
        this.uiBlock.unblock();
        this.saveLastRefreshDate();
        
        // Recargar biblioteca para ver cambios
        this.loadBiblioteca();
      },
      error: (err) => {
        clearInterval(progressInterval);
        console.error('Error al actualizar todos los juegos:', err);
        
        let errorMessage = 'No se pudo actualizar la biblioteca';
        
        if (err.error && typeof err.error === 'object') {
          if (err.error.mensaje) {
            errorMessage = err.error.mensaje;
          } else if (err.error.message) {
            errorMessage = err.error.message;
          }
        } else if (err.message) {
          errorMessage = err.message;
        }
        
        if (err.status === 0 || err.statusText === 'Unknown Error') {
          errorMessage = 'Tiempo de espera agotado. La actualización completa puede tardar varias horas con bibliotecas grandes.';
        }
        
        this.refreshAllProgress.set(`❌ Error: ${errorMessage}`);
        this.showRefreshAllComplete.set(true);
        this.refreshingAllGames.set(false);
        this.isOperationInProgress.set(false);
        this.uiBlock.unblock();
      }
    });
  }

  /**
   * Cierra el mensaje de actualización completa
   */
  closeRefreshAllMessage(): void {
    this.showRefreshAllComplete.set(false);
    this.refreshAllProgress.set('');
    this.isOperationInProgress.set(false);
    this.uiBlock.unblock();
  }

  /**
   * Cierra el mensaje de sincronización
   */
  closeSyncMessage(): void {
    this.showSyncComplete.set(false);
    this.syncProgress.set('');
    this.isOperationInProgress.set(false);
    this.uiBlock.unblock();
  }
}

