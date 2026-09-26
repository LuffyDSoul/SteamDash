// Core interfaces per specification
export interface Game {
  appId: number;
  name: string;
  tags: string[];
  headerImage: string;
  imgVertical?: string; // URL vertical image: https://cdn.akamai.steamstatic.com/steam/apps/<appid>/library_600x900.jpg
  imgIconUrl?: string; // URL del icono pequeño (de GetOwnedGames)
  hours: number;
  owners?: number;
  ownersList?: Array<{ steamId: string; avatar: string; personaName: string }>; // Lista de usuarios que lo tienen
  price?: string | null; // Precio del juego, ej: "$19.99", "Free to Play"
  storeUrl?: string | null; // URL de la tienda de Steam
  // Optional compatibility fields for other components
  playtime_hours?: number;
  genres?: string[];
  copies?: number;
  hasAdultContent: boolean;
}

export interface SteamUser {
  steamId: string;
  personaName: string;
  avatar: string;
  accountCreated: string; // ISO format
  stats: { hours: number; games: number; uniques: number };
  countryCode?: string; // e.g., "US", "AR", etc.
  communityVisibilityState?: number; // 1=Private, 2=Friends Only, 3=Public
  isPrivate?: boolean; // Derived from communityVisibilityState !== 3
}

export interface CompareSlot {
  steamId?: string;
  personaName?: string;
  avatarUrl?: string;
  library?: { games?: Array<any> };
}

// BFF response DTOs (from actual API)
export interface BibliotecaComparacionDto {
  usuarios: UsuarioComparacionDto[];
  juegosComunes: JuegoComparacionDto[];
  juegosUnicosPorUsuario: Record<string, JuegoComparacionDto[]>;
  estadisticas?: EstadisticasDto;
}

export interface UsuarioComparacionDto {
  steamId: string;
  personaName?: string;
  avatarFull?: string;
  profileUrl?: string;
  localCountryCode?: string;
  timeCreated?: number;
  totalHorasJugadas?: number;
}

export interface JuegoComparacionDto {
  appId?: number;
  name?: string;
  headerImage?: string | null;
  'img-vertical'?: string | null;
  imgVertical?: string | null;
  imgIconUrl?: string | null;
  isFree?: boolean | null;
  price?: string | null;
  storeUrl?: string | null;
  tags?: Array<{ tag?: string }> | string[];
  cantidadCopias?: number;
  tiempoJugadoPorUsuario?: Record<string, number>;
  'appdetails-failed'?: boolean; // true si appdetails devolvió success: false
}

export interface EstadisticasDto {
  juegosComunes?: number;
  totalUsuarios?: number;
  porcentajeSimilitud?: number;
  categoriaComparacion?: string;
}

// Helpers
export function mapJuegoToGame(juego: JuegoComparacionDto, steamId?: string): Game {
  const tags = Array.isArray(juego.tags)
    ? juego.tags.map(t => typeof t === 'string' ? t : (t as any).tag || '').filter(Boolean)
    : [];
  
  const hours = steamId && juego.tiempoJugadoPorUsuario ? (juego.tiempoJugadoPorUsuario[steamId] || 0) : 0;
  
  // Usar imagen vertical SOLO si el backend la provee explícitamente
  // No generar URLs por defecto si el backend devuelve null
  const backendVerticalImage = (juego as any)['img-vertical'] || juego.imgVertical;
  const verticalImage = (backendVerticalImage && backendVerticalImage !== 'null') ? backendVerticalImage : '';
  
  return {
    appId: juego.appId || 0,
    name: juego.name || 'Unknown',
    tags,
    headerImage: verticalImage, // SOLO imagen vertical del backend
    imgVertical: verticalImage,
    hours,
    owners: juego.cantidadCopias,
    hasAdultContent: false // TODO: detect from tags or separate field
  };
}

export function mapUsuarioToSteamUser(u: UsuarioComparacionDto, comp?: BibliotecaComparacionDto): SteamUser {
  // El backend puede usar personaName o steamId como key
  const userKey = u.personaName || u.steamId || '';
  const steamIdKey = u.steamId || '';
  
  // Intentar con ambas claves
  const uniqueGames = comp?.juegosUnicosPorUsuario?.[userKey] || 
                      comp?.juegosUnicosPorUsuario?.[steamIdKey] || 
                      [];
  
  const commonGames = comp?.juegosComunes || [];
  
  // Contar juegos comunes que este usuario tiene (buscar en tiempoJugadoPorUsuario)
  const commonGamesCount = commonGames.filter(g => {
    const playTimeMap = g.tiempoJugadoPorUsuario || {};
    return playTimeMap[userKey] !== undefined || playTimeMap[steamIdKey] !== undefined;
  }).length;
  
  const totalGames = uniqueGames.length + commonGamesCount;

  return {
    steamId: u.steamId || '',
    personaName: u.personaName || 'Unknown',
    avatar: u.avatarFull || '',
    accountCreated: u.timeCreated ? new Date(u.timeCreated * 1000).toISOString() : '',
    stats: {
      hours: Math.round(u.totalHorasJugadas || 0),
      games: totalGames,
      uniques: uniqueGames.length
    }
  };
}
