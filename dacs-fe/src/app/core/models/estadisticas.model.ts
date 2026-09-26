/**
 * Interfaz para las estadísticas completas de la biblioteca de un usuario
 */
export interface IEstadisticasBiblioteca {
  steamId: string;
  personaName?: string;
  avatarUrl?: string;
  totalJuegos: number;
  
  // Resumen general
  totalGastado: number;
  horasTotalesJugadas: number;
  horasUltimas2Semanas: number;
  juegosJugadosUltimas2Semanas: number;
  
  // Juegos sin jugar
  juegosSinJugar: IJuegosSinJugar;
  
  // Top 10 juegos más jugados
  top10MasJugados: IJuegoTop[];
  
  // Top 5 juegos jugados en las últimas 2 semanas
  top5Recientes: IJuegoTop[];
  
  // Clasificación gratuitos vs pagos
  clasificacionPrecio: IClasificacionPrecio;
  
  // Dinero desperdiciado
  dineroDesperdiciado: IDineroDesperdiciado;
  
  // Tags más comunes
  tagsMasComunes: ITagEstadistica[];
  
  // Estadísticas de logros (NUEVO)
  estadisticasLogros?: IAchievementStats;
}

/**
 * Estadísticas de logros de un usuario
 */
export interface IAchievementStats {
  steamId: string;
  totalJuegosConLogros: number;
  totalLogrosDesbloqueados: number;
  totalLogrosDisponibles: number;
  porcentajeGlobal: number;
  
  // Listas de juegos por categoría
  juegosCompletos100: IGameAchievementProgress[];
  juegosCercanos100: IGameAchievementProgress[];
  juegosMasProgreso: IGameAchievementProgress[]; // Basado en tiempo jugado
}

/**
 * Progreso de logros de un juego individual
 */
export interface IGameAchievementProgress {
  appId: number;
  totalAchievements: number;
  unlockedAchievements: number;
  percentage: number;
  
  // Información enriquecida del juego
  gameName?: string;
  headerImage?: string;
  playtimeForever?: number; // minutos
}

/**
 * Estadísticas de juegos sin jugar
 */
export interface IJuegosSinJugar {
  cantidad: number;
  porcentaje: number;
  lista?: IJuegoUsuarioBasico[]; // Lista opcional de los juegos sin jugar
}

/**
 * Juego en el top 10 más jugados
 */
export interface IJuegoTop {
  appId: number;
  name: string;
  horasTotales: number; // Convertido de minutos a horas
  fechaUltimoInicio?: string;
  headerImage?: string;
  playtimeForever: number; // minutos originales
  tags?: string[]; // Tags del juego para detectar contenido NSFW
  isBorrowed?: boolean; // Indica si es un juego de préstamo familiar
}

/**
 * Clasificación de juegos por precio
 */
export interface IClasificacionPrecio {
  juegosGratuitos: number;
  juegosPagos: number;
  porcentajeGratuitos: number;
  porcentajePagos: number;
}

/**
 * Información sobre dinero desperdiciado
 */
export interface IDineroDesperdiciado {
  totalDesperdiciado: number;
  cantidadJuegos: number;
  porcentajeDelGastoTotal: number;
  juegosMasCaros: IJuegoCaro[];
}

/**
 * Juego caro no jugado
 */
export interface IJuegoCaro {
  appId: number;
  name: string;
  precio: number;
  headerImage?: string;
}

/**
 * Estadística de un tag
 */
export interface ITagEstadistica {
  tag: string;
  cantidad: number;
  porcentaje: number;
}

/**
 * Información básica de un juego
 */
export interface IJuegoUsuarioBasico {
  appId: number;
  name: string;
  headerImage?: string;
  price?: string;
  isFree?: boolean;
  playtimeForever: number;
}
