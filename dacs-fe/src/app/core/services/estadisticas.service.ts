import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, forkJoin, catchError, of } from 'rxjs';
import { BaseApiService } from './base-api.service';
import { BibliotecaUsuarioService, IRecentlyPlayedGame } from './biblioteca-usuario.service';
import { 
  IEstadisticasBiblioteca, 
  IJuegosSinJugar, 
  IJuegoTop, 
  IClasificacionPrecio,
  IDineroDesperdiciado,
  ITagEstadistica,
  IJuegoCaro,
  IAchievementStats
} from '../models/estadisticas.model';
import { IBibliotecaUsuario, IJuegoUsuario } from '../models/biblioteca-usuario.model';

/**
 * Servicio para calcular estadísticas de la biblioteca de juegos
 */
@Injectable({
  providedIn: 'root'
})
export class EstadisticasService extends BaseApiService {
  
  constructor(
    protected override http: HttpClient,
    private bibliotecaService: BibliotecaUsuarioService
  ) {
    super(http);
  }

  /**
   * Obtiene las estadísticas completas de la biblioteca de un usuario
   * @param steamId Steam ID del usuario
   */
  getEstadisticas(steamId: string): Observable<IEstadisticasBiblioteca> {
    // Obtener ambas fuentes: biblioteca completa y juegos recientes
    return forkJoin({
      biblioteca: this.bibliotecaService.getBibliotecaUsuario(steamId, true),
      recentlyPlayed: this.bibliotecaService.getRecentlyPlayedGamesSteam(steamId),
      achievementStats: this.getAchievementStats(steamId).pipe(
        catchError(err => {
          console.warn('No se pudieron cargar estadísticas de logros:', err);
          return of(null);
        })
      )
    }).pipe(
      map(({ biblioteca, recentlyPlayed, achievementStats }) => {
        console.log('Biblioteca recibida:', biblioteca);
        console.log('Recently played recibido:', recentlyPlayed);
        console.log('Achievement stats recibido:', achievementStats);
        const stats = this.calcularEstadisticas(biblioteca, recentlyPlayed);
        if (achievementStats) {
          stats.estadisticasLogros = achievementStats;
        }
        return stats;
      })
    );
  }

  /**
   * Obtiene estadísticas de logros del usuario desde el BFF
   * @param steamId Steam ID del usuario
   */
  getAchievementStats(steamId: string): Observable<IAchievementStats> {
    return this.get<IAchievementStats>(`/usuarios/${steamId}/estadisticas/logros`);
  }

  /**
   * Sincroniza todos los logros de la biblioteca del usuario
   * @param steamId Steam ID del usuario
   */
  syncAchievements(steamId: string): Observable<string> {
    return this.http.post(`${this.baseUrl}/usuarios/${steamId}/estadisticas/logros/sync`, {}, { responseType: 'text' });
  }

  /**
   * Verifica si un juego debe ser excluido de los cálculos financieros
   * Excluye: Multiplayer, Multi-player, Multi Player, Server, Public Test, Demo, Unstable, Staging Branch
   * Whitelist: Rising Storm/Red Orchestra 2 Multiplayer
   */
  private debeExcluirJuego(nombre: string): boolean {
    if (!nombre) return false;
    
    const nombreLower = nombre.toLowerCase();
    
    // Whitelist: Juegos que NUNCA deben ser excluidos
    const whitelist = [
      'rising storm/red orchestra 2 multiplayer'
    ];
    if (whitelist.includes(nombreLower)) {
      return false;
    }
    
    // Palabras a excluir (incluyendo variaciones con guión y espacios)
    const palabrasExactas = ['multiplayer', 'multi-player', 'multi player', 'server', 'public test', 'unstable', 'staging branch'];
    
    // Verificar palabras exactas (como palabras completas)
    for (const palabra of palabrasExactas) {
      const regex = new RegExp(`\\b${palabra.replace(/[-\s]/g, '[-\\s]?')}\\b`, 'i');
      if (regex.test(nombreLower)) {
        return true;
      }
    }
    
    // Verificar "Demo" como palabra completa (no "Demon")
    const demoRegex = /\bdemo\b/i;
    if (demoRegex.test(nombreLower)) {
      return true;
    }
    
    return false;
  }

  /**
   * Calcula todas las estadísticas a partir de la biblioteca
   */
  private calcularEstadisticas(biblioteca: IBibliotecaUsuario, recentlyPlayed: IRecentlyPlayedGame[]): IEstadisticasBiblioteca {
    const juegos = biblioteca.juegos || [];
    
    return {
      steamId: biblioteca.steamId,
      personaName: biblioteca.personaName,
      avatarUrl: biblioteca.avatarUrl,
      totalJuegos: biblioteca.totalJuegos,
      totalGastado: this.calcularTotalGastado(juegos),
      horasTotalesJugadas: this.calcularHorasTotales(juegos),
      horasUltimas2Semanas: this.calcularHorasUltimas2Semanas(juegos),
      juegosJugadosUltimas2Semanas: this.calcularJuegosJugadosUltimas2Semanas(juegos),
      juegosSinJugar: this.calcularJuegosSinJugar(juegos, biblioteca.totalJuegos),
      top10MasJugados: this.calcularTop10MasJugados(juegos),
      top5Recientes: this.calcularTop5Recientes(juegos, recentlyPlayed),
      clasificacionPrecio: this.calcularClasificacionPrecio(juegos, biblioteca.totalJuegos),
      dineroDesperdiciado: this.calcularDineroDesperdiciado(juegos),
      tagsMasComunes: this.calcularTagsMasComunes(juegos, biblioteca.totalJuegos)
    };
  }

  /**
   * Calcula el total gastado en todos los juegos
   */
  private calcularTotalGastado(juegos: IJuegoUsuario[]): number {
    const total = juegos
      .filter(j => !j.isFree && !this.debeExcluirJuego(j.name))
      .map(j => this.extraerPrecio(j.price))
      .reduce((sum, precio) => sum + precio, 0);
    
    return Math.round(total * 100) / 100;
  }

  /**
   * Calcula las horas totales jugadas en toda la biblioteca
   */
  private calcularHorasTotales(juegos: IJuegoUsuario[]): number {
    const minutosTotales = juegos
      .map(j => j.playtimeForever || 0)
      .reduce((sum, minutos) => sum + minutos, 0);
    
    // Convertir minutos a horas
    return Math.round((minutosTotales / 60) * 10) / 10;
  }

  /**
   * Calcula las horas jugadas en las últimas 2 semanas
   */
  private calcularHorasUltimas2Semanas(juegos: IJuegoUsuario[]): number {
    const minutosTotales = juegos
      .map(j => j.playtime2Weeks || 0)
      .reduce((sum, minutos) => sum + minutos, 0);
    
    // Convertir minutos a horas
    return Math.round((minutosTotales / 60) * 10) / 10;
  }

  /**
   * Calcula la cantidad de juegos jugados en las últimas 2 semanas
   */
  private calcularJuegosJugadosUltimas2Semanas(juegos: IJuegoUsuario[]): number {
    return juegos.filter(j => (j.playtime2Weeks || 0) > 0).length;
  }

  /**
   * Calcula estadísticas de juegos sin jugar (0 horas)
   */
  private calcularJuegosSinJugar(juegos: IJuegoUsuario[], totalJuegos: number): IJuegosSinJugar {
    const sinJugar = juegos.filter(j => (j.playtimeForever || 0) === 0);
    const cantidad = sinJugar.length;
    const porcentaje = totalJuegos > 0 ? (cantidad / totalJuegos) * 100 : 0;
    
    return {
      cantidad,
      porcentaje: Math.round(porcentaje * 100) / 100,
      lista: sinJugar.map(j => ({
        appId: j.appId,
        name: j.name,
        headerImage: j.headerImage,
        price: j.price,
        isFree: j.isFree,
        playtimeForever: j.playtimeForever
      }))
    };
  }

  /**
   * Calcula el top 10 de juegos más jugados
   */
  private calcularTop10MasJugados(juegos: IJuegoUsuario[]): IJuegoTop[] {
    return juegos
      .filter(j => (j.playtimeForever || 0) > 0)
      .sort((a, b) => (b.playtimeForever || 0) - (a.playtimeForever || 0))
      .slice(0, 10)
      .map(j => ({
        appId: j.appId,
        name: j.name,
        horasTotales: Math.round((j.playtimeForever / 60) * 100) / 100,
        fechaUltimoInicio: undefined, // No disponible en el modelo actual
        headerImage: j.headerImage,
        playtimeForever: j.playtimeForever,
        tags: j.tags, // Incluir tags para detectar contenido NSFW
        isBorrowed: j.isBorrowed || false // Incluir indicador de préstamo familiar
      }));
  }

  /**
   * Calcula el top 5 de juegos jugados en las últimas 2 semanas
   */
  /**
   * Calcula el top 5 de juegos jugados recientemente
   * Combina datos de la biblioteca con recently played para incluir juegos de préstamo
   */
  private calcularTop5Recientes(juegos: IJuegoUsuario[], recentlyPlayed: IRecentlyPlayedGame[]): IJuegoTop[] {
    console.log('=== DEBUG calcularTop5Recientes ===');
    console.log('Juegos de biblioteca:', juegos.length);
    console.log('RecentlyPlayed recibido:', recentlyPlayed);
    console.log('Juegos con playtime2Weeks en biblioteca:', 
      juegos.filter(j => (j.playtime2Weeks || 0) > 0).map(j => ({ 
        appId: j.appId, 
        name: j.name, 
        playtime2Weeks: j.playtime2Weeks 
      }))
    );
    
    // Crear un mapa de juegos de la biblioteca por appId para búsqueda rápida
    const juegosPorAppId = new Map<number, IJuegoUsuario>();
    juegos.forEach(j => juegosPorAppId.set(j.appId, j));

    // Crear un mapa para almacenar el tiempo jugado y nombre por appId (combinando ambas fuentes)
    const tiempoPorAppId = new Map<number, { playtime2Weeks: number, name: string }>();

    // 1. Agregar juegos de recentlyPlayed (incluye juegos prestados)
    if (recentlyPlayed && recentlyPlayed.length > 0) {
      console.log('Procesando recentlyPlayed:', recentlyPlayed.length, 'juegos');
      recentlyPlayed.forEach(rp => {
        if (rp && rp.playtime2Weeks > 0) {
          tiempoPorAppId.set(rp.appId, { playtime2Weeks: rp.playtime2Weeks, name: rp.name });
          console.log(`  - AppId ${rp.appId} (${rp.name}): ${rp.playtime2Weeks} minutos`);
        }
      });
    } else {
      console.log('No hay datos de recentlyPlayed');
    }

    // 2. Agregar/actualizar con juegos de la biblioteca que tienen playtime2Weeks
    let juegosBibliotecaConTiempo = 0;
    juegos.forEach(j => {
      if ((j.playtime2Weeks || 0) > 0) {
        juegosBibliotecaConTiempo++;
        // Usar el máximo entre los dos si el juego está en ambas fuentes
        const tiempoActual = tiempoPorAppId.get(j.appId)?.playtime2Weeks || 0;
        const nuevoTiempo = Math.max(tiempoActual, j.playtime2Weeks || 0);
        tiempoPorAppId.set(j.appId, { playtime2Weeks: nuevoTiempo, name: j.name });
        console.log(`  - Biblioteca AppId ${j.appId} (${j.name}): ${j.playtime2Weeks} minutos, final: ${nuevoTiempo}`);
      }
    });
    console.log('Juegos de biblioteca con playtime2Weeks:', juegosBibliotecaConTiempo);
    console.log('Total juegos únicos con tiempo reciente:', tiempoPorAppId.size);

    // 3. Crear lista combinada con todos los juegos que tienen tiempo reciente
    const juegosCombinados: IJuegoTop[] = Array.from(tiempoPorAppId.entries())
      .map(([appId, data]) => {
        const juegoEnBiblioteca = juegosPorAppId.get(appId);
        
        if (juegoEnBiblioteca) {
          // Juego existe en biblioteca, usar sus datos completos
          return {
            appId: appId,
            name: juegoEnBiblioteca.name,
            horasTotales: Math.round((data.playtime2Weeks / 60) * 100) / 100,
            fechaUltimoInicio: undefined,
            headerImage: juegoEnBiblioteca.headerImage || `https://cdn.akamai.steamstatic.com/steam/apps/${appId}/header.jpg`,
            playtimeForever: data.playtime2Weeks,
            tags: juegoEnBiblioteca.tags,
            isBorrowed: juegoEnBiblioteca.isBorrowed || false // Usar el flag del juego
          };
        } else {
          // Juego de préstamo familiar u otra fuente - usar nombre de recentlyPlayed
          return {
            appId: appId,
            name: data.name, // Usar nombre real de Steam
            horasTotales: Math.round((data.playtime2Weeks / 60) * 100) / 100,
            fechaUltimoInicio: undefined,
            headerImage: `https://cdn.akamai.steamstatic.com/steam/apps/${appId}/header.jpg`,
            playtimeForever: data.playtime2Weeks,
            tags: [],
            isBorrowed: true
          };
        }
      });

    // 4. Ordenar por horas jugadas y tomar top 5
    const top5 = juegosCombinados
      .sort((a, b) => b.horasTotales - a.horasTotales)
      .slice(0, 5);
    
    console.log('Top 5 final:', top5.map(j => ({ name: j.name, horas: j.horasTotales, borrowed: j.isBorrowed })));
    console.log('=== FIN DEBUG ===');
    
    return top5;
  }

  /**
   * Calcula clasificación de juegos gratuitos vs pagos
   */
  private calcularClasificacionPrecio(juegos: IJuegoUsuario[], totalJuegos: number): IClasificacionPrecio {
    // Contar gratuitos: juegos con isFree=true O precio "Gratis"
    const gratuitos = juegos.filter(j => 
      j.isFree === true || 
      j.price === 'Gratis'
    ).length;
    
    // Contar pagos: el resto de los juegos (total - gratuitos)
    const pagos = totalJuegos - gratuitos;
    
    return {
      juegosGratuitos: gratuitos,
      juegosPagos: pagos,
      porcentajeGratuitos: totalJuegos > 0 ? Math.round((gratuitos / totalJuegos) * 100 * 100) / 100 : 0,
      porcentajePagos: totalJuegos > 0 ? Math.round((pagos / totalJuegos) * 100 * 100) / 100 : 0
    };
  }

  /**
   * Calcula dinero desperdiciado en juegos no jugados
   */
  private calcularDineroDesperdiciado(juegos: IJuegoUsuario[]): IDineroDesperdiciado {
    const sinJugar = juegos.filter(j => 
      (j.playtimeForever || 0) === 0 && 
      !j.isFree && 
      !this.debeExcluirJuego(j.name)
    );
    
    // Extraer precios
    const juegosPagosNoJugados = sinJugar
      .map(j => ({
        juego: j,
        precio: this.extraerPrecio(j.price)
      }))
      .filter(item => item.precio > 0);

    const totalDesperdiciado = juegosPagosNoJugados.reduce((sum, item) => sum + item.precio, 0);
    
    // Calcular gasto total
    const todosLosPagos = juegos
      .filter(j => !j.isFree && !this.debeExcluirJuego(j.name))
      .map(j => this.extraerPrecio(j.price))
      .reduce((sum, precio) => sum + precio, 0);

    const porcentaje = todosLosPagos > 0 ? (totalDesperdiciado / todosLosPagos) * 100 : 0;

    // Juegos más caros no jugados - TODOS ordenados por precio
    const masCaros = juegosPagosNoJugados
      .sort((a, b) => b.precio - a.precio)
      .map(item => ({
        appId: item.juego.appId,
        name: item.juego.name,
        precio: item.precio,
        headerImage: item.juego.headerImage
      }));

    return {
      totalDesperdiciado: Math.round(totalDesperdiciado * 100) / 100,
      cantidadJuegos: juegosPagosNoJugados.length,
      porcentajeDelGastoTotal: Math.round(porcentaje * 100) / 100,
      juegosMasCaros: masCaros
    };
  }

  /**
   * Calcula los tags más comunes en la biblioteca
   */
  private calcularTagsMasComunes(juegos: IJuegoUsuario[], totalJuegos: number): ITagEstadistica[] {
    const tagCount = new Map<string, number>();
    const tagJuegos = new Map<string, Set<number>>(); // Para contar juegos únicos por tag

    juegos.forEach(j => {
      if (j.tags && Array.isArray(j.tags)) {
        j.tags.forEach(tag => {
          const tagLimpio = tag.trim();
          if (tagLimpio) {
            if (!tagJuegos.has(tagLimpio)) {
              tagJuegos.set(tagLimpio, new Set());
            }
            tagJuegos.get(tagLimpio)!.add(j.appId);
          }
        });
      }
    });

    // Convertir a array y ordenar por cantidad
    tagJuegos.forEach((appIds, tag) => {
      tagCount.set(tag, appIds.size);
    });

    return Array.from(tagCount.entries())
      .map(([tag, cantidad]) => ({
        tag,
        cantidad,
        porcentaje: totalJuegos > 0 ? Math.round((cantidad / totalJuegos) * 100 * 100) / 100 : 0
      }))
      .sort((a, b) => b.cantidad - a.cantidad)
      .slice(0, 20); // Top 20 tags
  }

  /**
   * Extrae el precio numérico de una cadena de precio
   * Soporta formatos: "$19.99", "19.99", "USD 19.99", etc.
   */
  private extraerPrecio(precio?: string): number {
    if (!precio) return 0;
    
    // Eliminar texto como "USD", "$", "ARS", etc. y quedarse solo con números
    const match = precio.match(/[\d,.]+/);
    if (!match) return 0;
    
    const numeroStr = match[0].replace(/,/g, ''); // Eliminar comas
    const numero = parseFloat(numeroStr);
    
    return isNaN(numero) ? 0 : numero;
  }
}
