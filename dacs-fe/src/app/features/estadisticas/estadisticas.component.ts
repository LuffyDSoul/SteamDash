import { Component, OnInit, signal, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { EstadisticasService } from '../../core/services/estadisticas.service';
import { ContentFilterService } from '../../core/services/content-filter.service';
import { UiBlockService } from '../../core/services/ui-block.service';
import { IEstadisticasBiblioteca } from '../../core/models/estadisticas.model';

@Component({
  selector: 'app-estadisticas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './estadisticas.component.html',
  styleUrls: ['./estadisticas.component.css']
})
export class EstadisticasComponent implements OnInit {
  steamId = signal<string>('');
  estadisticas = signal<IEstadisticasBiblioteca | null>(null);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  inputSteamId: string = '';
  
  // Set para trackear juegos con blur removido
  unblurredGames = signal<Set<number>>(new Set());
  
  // Control para mostrar todos los tags o solo los primeros 6
  mostrarTodosTags = signal<boolean>(false);
  
  // Control para sincronización de logros
  syncingAchievements = signal<boolean>(false);
  achievementSyncMessage = signal<string | null>(null);
  showAchievementSyncComplete = signal<boolean>(false);
  isOperationInProgress = signal<boolean>(false);
  
  private uiBlock = inject(UiBlockService);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private estadisticasService: EstadisticasService,
    private contentFilter: ContentFilterService
  ) {}

  ngOnInit(): void {
    // Intentar obtener steamId de la ruta
    this.route.paramMap.subscribe(params => {
      const steamIdFromRoute = params.get('steamId');
      if (steamIdFromRoute) {
        this.steamId.set(steamIdFromRoute);
        this.inputSteamId = steamIdFromRoute;
        this.loadEstadisticas();
      }
    });
  }

  /**
   * Previene que el usuario abandone la página durante operaciones largas
   */
  @HostListener('window:beforeunload', ['$event'])
  unloadNotification($event: any): void {
    if (this.isOperationInProgress()) {
      $event.returnValue = '¿Estás seguro? Hay una sincronización de logros en progreso que se perderá si sales de la página.';
    }
  }

  /**
   * Carga las estadísticas del usuario
   */
  loadEstadisticas(): void {
    const id = this.steamId();
    if (!id || id.trim() === '') {
      this.error.set('Por favor ingresa un Steam ID válido');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.estadisticas.set(null);
    
    // Resetear estados visuales
    this.unblurredGames.set(new Set());
    this.mostrarTodosTags.set(false);

    this.estadisticasService.getEstadisticas(id).subscribe({
      next: (data) => {
        this.estadisticas.set(data);
        this.loading.set(false);
        console.log('Estadísticas cargadas:', data);
        console.log('Top 5 Recientes:', data.top5Recientes);
        console.log('Juegos con playtime2Weeks > 0:', 
          data.top5Recientes.map(j => ({ 
            name: j.name, 
            playtime2Weeks: j.playtimeForever 
          }))
        );
      },
      error: (err) => {
        console.error('Error al cargar estadísticas:', err);
        this.error.set(err.message || 'Error al cargar las estadísticas del usuario');
        this.loading.set(false);
      }
    });
  }

  /**
   * Busca estadísticas por Steam ID ingresado
   */
  buscarEstadisticas(): void {
    const id = this.inputSteamId.trim();
    if (id) {
      const currentId = this.steamId();
      this.steamId.set(id);
      
      // Si es el mismo usuario, forzar recarga
      if (currentId === id) {
        this.loadEstadisticas();
      } else {
        this.router.navigate(['/estadisticas', id]);
      }
    }
  }

  /**
   * Navega a la biblioteca del usuario
   */
  irABiblioteca(): void {
    const id = this.steamId();
    if (id) {
      this.router.navigate(['/biblioteca', id]);
    }
  }

  /**
   * Abre la página de Steam del juego
   */
  abrirEnSteam(appId: number): void {
    window.open(`https://store.steampowered.com/app/${appId}`, '_blank');
  }

  /**
   * Formatea horas con decimales
   */
  formatHoras(horas: number): string {
    if (horas === 0) return '0h';
    if (horas < 1) return `${Math.round(horas * 60)}m`;
    return `${horas.toFixed(1)}h`;
  }

  /**
   * Formatea horas totales de forma más legible (para el user card)
   */
  formatHorasCompletas(horas: number): string {
    if (horas === 0) return '0h';
    if (horas < 1) return `${Math.round(horas * 60)}m`;
    if (horas < 100) return `${horas.toFixed(1)}h`;
    return `${Math.round(horas)}h`;
  }

  /**
   * Formatea precio con símbolo de moneda
   */
  formatPrecio(precio: number): string {
    return `$${precio.toFixed(2)}`;
  }

  /**
   * Formatea porcentaje
   */
  formatPorcentaje(porcentaje: number): string {
    return `${porcentaje.toFixed(1)}%`;
  }

  /**
   * Obtiene la URL de la imagen header del juego
   * Para el top de recientes, prioriza las imágenes verticales (library)
   */
  getHeaderImage(appId: number, headerImage?: string): string {
    return headerImage || `https://cdn.akamai.steamstatic.com/steam/apps/${appId}/header.jpg`;
  }

  /**
   * Obtiene la URL de la imagen vertical (capsule) del juego para el top recientes
   */
  getLibraryImage(appId: number, headerImage?: string): string {
    // Si headerImage contiene "library_600x900", usar esa
    if (headerImage && headerImage.includes('library_600x900')) {
      return headerImage;
    }
    // Sino, intentar con la URL de library, y si falla usar header normal
    return `https://cdn.akamai.steamstatic.com/steam/apps/${appId}/header.jpg`;
  }

  /**
   * Verifica si un juego es de préstamo familiar
   */
  isGameBorrowed(juego: any): boolean {
    return juego?.isBorrowed === true;
  }

  /**
   * Verifica si un juego tiene contenido NSFW usando el filtro Atlas compartido
   */
  isNSFWGame(tags?: string[], name?: string): boolean {
    return this.contentFilter.isAdultContent(name || '', tags || []);
  }

  /**
   * Verifica si un juego específico tiene el blur removido
   */
  isGameUnblurred(appId: number): boolean {
    return this.unblurredGames().has(appId);
  }

  /**
   * Toggle del blur para un juego específico
   */
  toggleBlur(appId: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    
    const current = this.unblurredGames();
    const newSet = new Set(current);
    
    if (newSet.has(appId)) {
      newSet.delete(appId);
    } else {
      newSet.add(appId);
    }
    
    this.unblurredGames.set(newSet);
  }

  /**
   * Toggle para mostrar/ocultar todos los tags
   */
  toggleMostrarTags(): void {
    this.mostrarTodosTags.set(!this.mostrarTodosTags());
  }

  /**
   * Obtiene los tags a mostrar (6 primeros o todos)
   */
  getTagsAMostrar() {
    const tags = this.estadisticas()?.tagsMasComunes || [];
    return this.mostrarTodosTags() ? tags : tags.slice(0, 6);
  }

  /**
   * Sincroniza todos los logros de la biblioteca del usuario
   */
  syncAllAchievements(): void {
    const id = this.steamId();
    if (!id) return;

    const numGames = this.estadisticas()?.totalJuegos || 0;
    const estimatedTime = numGames * 1; // 1 segundo por juego

    this.isOperationInProgress.set(true);
    this.uiBlock.block();
    this.syncingAchievements.set(true);
    this.achievementSyncMessage.set('🔄 Iniciando sincronización de logros...');
    this.showAchievementSyncComplete.set(false);

    this.estadisticasService.syncAchievements(id).subscribe({
      next: (message) => {
        // El proceso continúa - esperar 1 segundo por cada juego
        this.achievementSyncMessage.set(
          `⏳ Sincronizando logros de ${numGames} juegos... ` +
          `Tiempo estimado: ${estimatedTime} segundos.`
        );
        
        // Mantener la animación breathing durante el tiempo calculado (1 segundo por juego)
        setTimeout(() => {
          this.syncingAchievements.set(false);
          this.isOperationInProgress.set(false);
          this.uiBlock.unblock();
          this.achievementSyncMessage.set('✅ Sincronización completada. Puedes cerrar este mensaje.');
          // Ahora sí aparece el botón X y se desbloquea la UI
        }, estimatedTime * 1000);
      },
      error: (err) => {
        console.error('Error al sincronizar logros:', err);
        this.achievementSyncMessage.set('❌ Error al sincronizar logros: ' + (err.message || 'Error desconocido'));
        this.syncingAchievements.set(false);
        this.isOperationInProgress.set(false);
        this.uiBlock.unblock();
      }
    });
  }

  /**
   * Cierra el mensaje de sincronización de logros
   */
  closeAchievementSyncMessage(): void {
    this.achievementSyncMessage.set(null);
    this.showAchievementSyncComplete.set(false);
    // No tocar isOperationInProgress ni syncingAchievements - ya fueron desbloqueados cuando terminó el setTimeout
    
    // Recargar estadísticas para mostrar los logros actualizados
    this.loadEstadisticas();
  }

  /**
   * Verifica si hay estadísticas de logros disponibles
   */
  hasAchievementStats(): boolean {
    const stats = this.estadisticas()?.estadisticasLogros;
    return stats != null && stats.totalJuegosConLogros > 0;
  }
}

