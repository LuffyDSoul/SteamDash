/**
 * Interfaz para Usuario
 */
export interface IUsuario {
  idUsuario?: number;
  steamId?: string;
  nombre?: string;
  apellido?: string;
  email?: string;
  nickname?: string;
  avatarUrl?: string;
  activo?: boolean;
}

/**
 * Interfaz para la comparación de bibliotecas
 */
export interface IBibliotecaComparacion {
  usuario1: IUsuarioComparacion;
  usuario2: IUsuarioComparacion;
  estadisticas: IEstadisticasComparacion;
  juegosComunes: IJuegoComparacion[];
  juegosSoloUsuario1: IJuegoComparacion[];
  juegosSoloUsuario2: IJuegoComparacion[];
}

/**
 * Interfaz para usuario en comparación
 */
export interface IUsuarioComparacion {
  steamId?: string;
  // backend uses personaName; frontend used nickname/nombre previously — aceptar ambos
  personaName?: string;
  nickname?: string;
  nombre?: string;
  avatarUrl?: string;
  avatarFull?: string;
  // backend uses totalHorasJugadas (horas), frontend used totalJuegos/tiempoTotalJugado
  totalHorasJugadas?: number; // en horas
  totalJuegos?: number;
  tiempoTotalJugado?: number; // en minutos (compatibilidad)
}

/**
 * Interfaz para estadísticas de comparación
 */
export interface IEstadisticasComparacion {
  juegosComunes?: number;
  juegosSoloUsuario1?: number;
  juegosSoloUsuario2?: number;
  porcentajeSimilitud?: number;
  categoriaComparacion?: string;
}

/**
 * Interfaz para juego en comparación
 */
export interface IJuegoComparacion {
  appId?: number | string;
  // backend uses 'name' y 'headerImage'
  name?: string;
  nombre?: string; // compat
  headerImage?: string;
  'img-vertical'?: string; // Imagen vertical desde backend
  imgVertical?: string; // Imagen vertical (camelCase)
  urlIcono?: string; // compat
  isFree?: boolean | null;
  esJuegoGratuito?: boolean | null; // compat
  price?: string | null;
  storeUrl?: string | null;
  tiempoJugadoUsuario1?: number; // en minutos
  tiempoJugadoUsuario2?: number; // en minutos
  tags?: ITag[] | string[];
}

export interface ITag {
  tag?: string;
}

/**
 * Interfaz para juego único enriquecido desde BFF/backend
 */
export interface IBibliotecaJuego {
  appId: number;
  name?: string | null;
  headerImage?: string | null;
  imgVertical?: string | null;
  imgIconUrl?: string | null;
  isFree?: boolean | null;
  price?: string | null;
  storeUrl?: string | null;
  tags?: ITag[] | string[] | null;
}