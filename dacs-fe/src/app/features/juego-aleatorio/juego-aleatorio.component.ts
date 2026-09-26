import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule, NgIf, NgFor } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BibliotecaUsuarioService } from '../../core/services/biblioteca-usuario.service';
import { IJuegoAleatorio } from '../../core/models/biblioteca-usuario.model';

@Component({
  selector: 'app-juego-aleatorio',
  standalone: true,
  imports: [CommonModule, FormsModule, NgIf, NgFor],
  templateUrl: './juego-aleatorio.component.html',
  styleUrls: ['./juego-aleatorio.component.css']
})
export class JuegoAleatorioComponent implements OnInit {
  private readonly LAST_GAME_KEY = 'juego-aleatorio-last-appid';
  
  steamId = signal<string>('');
  maxHoras = signal<number | null>(null);
  juego = signal<IJuegoAleatorio | null>(null);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);

  // Para el filtro de horas
  filtroHoras = signal<string>('no-jugados'); // 'no-jugados' | 'personalizado'
  horasPersonalizadas = signal<number>(5);
  
  // Para el filtro de tags
  selectedTags = signal<Set<string>>(new Set());
  availableTags = signal<string[]>([]);
  tagSearchText = signal<string>('');
  filtersOpen = signal<boolean>(false);
  filtersPanelPosition = signal<{ top: string; left: string }>({ top: '0px', left: '0px' });

  @ViewChild('filtersButton', { read: ElementRef }) filtersButton?: ElementRef;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private bibliotecaService: BibliotecaUsuarioService
  ) {}

  ngOnInit(): void {
    // Obtener steamId de la ruta
    this.route.paramMap.subscribe(params => {
      const steamId = params.get('steamId');
      if (steamId) {
        this.steamId.set(steamId);
        // Cargar tags disponibles de la biblioteca
        this.loadAvailableTags();
        // NO buscar automáticamente, esperar que el usuario seleccione criterios
      } else {
        this.error.set('Steam ID no proporcionado');
      }
    });
  }

  /**
   * Busca un juego aleatorio según los criterios actuales
   */
  buscarJuegoAleatorio(): void {
    const steamId = this.steamId();
    if (!steamId) {
      this.error.set('Steam ID no válido');
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.juego.set(null);

    // Determinar maxHoras según el filtro seleccionado
    let maxHoras: number | undefined = undefined;
    if (this.filtroHoras() === 'personalizado') {
      maxHoras = this.horasPersonalizadas();
    } else {
      maxHoras = undefined; // null = solo no jugados
    }

    // Obtener tags seleccionadas
    const tags = Array.from(this.selectedTags());
    console.log('🏷️ Tags seleccionados para búsqueda:', tags);
    console.log('🕐 MaxHoras:', maxHoras);

    // Obtener último juego para excluirlo
    const lastAppId = this.getLastGameAppId();
    if (lastAppId) {
      console.log('🚫 Excluyendo último juego:', lastAppId);
    }

    this.bibliotecaService.getJuegoAleatorio(steamId, maxHoras, tags, lastAppId || undefined).subscribe({
      next: (data) => {
        this.juego.set(data);
        this.loading.set(false);
        // Guardar appId en localStorage para evitar repetición inmediata
        if (data?.appId) {
          localStorage.setItem(this.LAST_GAME_KEY, data.appId.toString());
        }
        console.log('Juego aleatorio obtenido:', data);
      },
      error: (err) => {
        console.error('Error al buscar juego aleatorio:', err);
        // Mensaje específico según el error
        let errorMsg = 'Error al buscar juego aleatorio';
        if (err.status === 404 || err.error?.message?.includes('No se encontr')) {
          errorMsg = 'No se encontraron juegos que cumplan con los criterios seleccionados.';
          if (tags.length > 0) {
            errorMsg += ` Intenta con menos tags o diferentes criterios de horas.`;
          }
        } else {
          errorMsg = err.error?.message || err.message || errorMsg;
        }
        this.error.set(errorMsg);
        this.loading.set(false);
      }
    });
  }

  /**
   * Vuelve a la biblioteca del usuario
   */
  volverABiblioteca(): void {
    this.router.navigate(['/biblioteca', this.steamId()]);
  }

  /**
   * Abre la página de Steam del juego
   */
  abrirEnSteam(): void {
    const juego = this.juego();
    if (juego?.storeUrl) {
      window.open(juego.storeUrl, '_blank');
    }
  }

  /**
   * Obtiene el appId del último juego mostrado
   */
  private getLastGameAppId(): number | null {
    const lastAppId = localStorage.getItem(this.LAST_GAME_KEY);
    return lastAppId ? parseInt(lastAppId, 10) : null;
  }

  /**
   * Formatea minutos a horas y minutos
   */
  formatPlaytime(minutes?: number): string {
    if (!minutes || minutes === 0) return '0 horas';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours === 0) return `${mins} min`;
    if (mins === 0) return `${hours} hrs`;
    return `${hours} hrs ${mins} min`;
  }

  /**
   * Obtiene la imagen principal del juego (vertical si está disponible, sino header)
   */
  getMainImage(): string {
    const juego = this.juego();
    if (!juego) return '';
    return juego.libraryImage || juego.headerImage || `https://cdn.akamai.steamstatic.com/steam/apps/${juego.appId}/library_600x900.jpg`;
  }

  /**
   * Obtiene la imagen de fondo
   */
  getBackgroundImage(): string {
    const juego = this.juego();
    if (!juego) return '';
    return juego.backgroundImage || juego.headerImage || '';
  }

  /**
   * Cambia el tipo de filtro
   */
  cambiarFiltro(tipo: 'no-jugados' | 'personalizado'): void {
    this.filtroHoras.set(tipo);
  }

  /**
   * Actualiza las horas personalizadas y las guarda
   */
  actualizarHorasPersonalizadas(horas: number): void {
    this.horasPersonalizadas.set(horas);
  }

  /**
   * Alterna el panel de filtros de tags
   */
  toggleFilters(): void {
    const newState = !this.filtersOpen();
    this.filtersOpen.set(newState);
    if (newState && this.filtersButton) {
      const btn = this.filtersButton.nativeElement as HTMLElement;
      const rect = btn.getBoundingClientRect();
      
      // Ancho aproximado del panel (definido en CSS: min(520px, 95vw))
      const panelWidth = Math.min(520, window.innerWidth * 0.95);
      
      // Calcular posición, ajustando si se sale del viewport
      let left = rect.left;
      let top = rect.bottom + 8;
      
      // Si el panel se sale por la derecha, alinearlo a la derecha del botón
      if (left + panelWidth > window.innerWidth) {
        left = Math.max(8, rect.right - panelWidth);
      }
      
      // Asegurar que no se salga por la izquierda
      if (left < 8) {
        left = 8;
      }
      
      this.filtersPanelPosition.set({ top: `${top}px`, left: `${left}px` });
    }
  }

  /**
   * Cierra el panel de filtros
   */
  closeFilters(): void {
    this.filtersOpen.set(false);
  }

  /**
   * Cambia selección de un tag
   */
  toggleTag(tag: string): void {
    const curr = new Set(this.selectedTags());
    if (curr.has(tag)) curr.delete(tag); else curr.add(tag);
    this.selectedTags.set(curr);
  }

  /**
   * Limpia todos los filtros de tags
   */
  clearTagFilters(): void {
    this.selectedTags.set(new Set());
    this.tagSearchText.set('');
  }

  /**
   * Tags disponibles filtradas por el texto de búsqueda
   */
  filteredAvailableTags(): string[] {
    const txt = this.tagSearchText().toLowerCase().trim();
    const all = this.availableTags();
    return !txt ? all : all.filter(t => t.toLowerCase().includes(txt));
  }

  /**
   * Obtiene array de tags seleccionados para mostrar
   */
  getSelectedTagsArray(): string[] {
    return Array.from(this.selectedTags());
  }

  /**
   * Carga las tags disponibles desde la biblioteca del usuario
   */
  private loadAvailableTags(): void {
    const steamId = this.steamId();
    if (!steamId) return;

    this.bibliotecaService.getBibliotecaUsuario(steamId, false).subscribe({
      next: (biblioteca) => {
        // Extraer todas las tags únicas de los juegos
        const tagsSet = new Set<string>();
        biblioteca.juegos.forEach(juego => {
          juego.tags?.forEach(tag => tagsSet.add(tag));
        });
        
        // Convertir a array y ordenar alfabéticamente
        const tagsArray = Array.from(tagsSet).sort();
        this.availableTags.set(tagsArray);
        console.log('📦 Tags disponibles cargadas:', tagsArray.length);
      },
      error: (err) => {
        console.error('Error al cargar tags disponibles:', err);
        // No mostramos error al usuario, simplemente las tags no estarán disponibles
      }
    });
  }
}
