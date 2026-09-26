/**
 * Interfaz para la respuesta de la biblioteca del usuario
 */
export interface IBibliotecaUsuario {
  steamId: string;
  personaName?: string;
  avatarUrl?: string;
  totalJuegos: number;
  juegos: IJuegoUsuario[];
}

/**
 * Interfaz para un juego en la biblioteca del usuario
 */
export interface IJuegoUsuario {
  appId: number;
  name: string;
  playtimeForever: number; // en minutos
  playtime2Weeks?: number; // en minutos
  headerImage?: string;
  price?: string;
  isFree?: boolean;
  storeUrl?: string;
  libraryImage?: string; // URL de library_600x900.jpg
  tags?: string[];
  isBorrowed?: boolean; // Indica si el juego es prestado de biblioteca familiar
}

/**
 * Interfaz para la respuesta de refresh de un juego
 */
export interface IRefreshJuegoResponse {
  appId: number;
  success: boolean;
  message?: string;
  juegoActualizado?: IJuegoUsuario;
}

/**
 * Interfaz para un juego aleatorio con información completa
 */
export interface IJuegoAleatorio {
  appId: number;
  name: string;
  description?: string;
  shortDescription?: string;
  headerImage?: string;
  libraryImage?: string; // URL de library_600x900.jpg (imagen vertical)
  price?: string;
  isFree?: boolean;
  storeUrl?: string;
  tags?: string[];
  screenshots?: string[]; // URLs de screenshots
  developers?: string[];
  publishers?: string[];
  releaseDate?: string;
  genres?: string[];
  categories?: string[];
  playtimeForever?: number; // minutos
  backgroundImage?: string; // Imagen de fondo
}

/**
 * Interfaz para un logro individual
 */
export interface ICombinedAchievement {
  apiname: string;
  displayName: string;
  description: string;
  icon: string;
  icongray: string;
  achieved: boolean;
  unlocktime: number;
  hidden: boolean;
}

/**
 * Interfaz para los logros de un juego
 */
export interface IGameAchievements {
  steamId: string;
  appId: number;
  gameName?: string;
  totalAchievements?: number;
  unlockedAchievements?: number;
  achievements?: ICombinedAchievement[];
  success: boolean;
  error?: string;
}
