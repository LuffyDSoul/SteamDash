export interface BibliotecaComparacion {
  usuario1: UsuarioComparacion;
  usuario2: UsuarioComparacion;
  estadisticas: EstadisticasComparacion;
  juegosComunes: JuegoComparacion[];
  juegosSoloUsuario1: JuegoComparacion[];
  juegosSoloUsuario2: JuegoComparacion[];
}

export interface UsuarioComparacion {
  steamId?: string;
  personaName?: string;
  nickname?: string;
  nombre?: string;
  avatarUrl?: string;
  avatarFull?: string;
  totalHorasJugadas?: number; // en horas
  totalJuegos?: number;
  tiempoTotalJugado?: number; // en minutos
}

export interface EstadisticasComparacion {
  juegosComunes?: number;
  juegosSoloUsuario1?: number;
  juegosSoloUsuario2?: number;
  porcentajeSimilitud?: number;
  categoriaComparacion?: string;
}

export interface JuegoComparacion {
  appId?: number | string;
  name?: string;
  nombre?: string;
  headerImage?: string;
  'img-vertical'?: string; // Imagen vertical desde backend
  imgVertical?: string; // Imagen vertical (camelCase)
  urlIcono?: string;
  isFree?: boolean | null;
  esJuegoGratuito?: boolean | null;
  price?: string | null;
  storeUrl?: string | null;
  tiempoJugadoUsuario1?: number;
  tiempoJugadoUsuario2?: number;
  tags?: Array<{ tag?: string }> | string[];
}
