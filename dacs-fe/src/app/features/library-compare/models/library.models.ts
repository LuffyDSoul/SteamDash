// Core interfaces per specification
export interface Game {
  appId: number;
  name: string;
  tags: string[];
  headerImage: string;
  hours: number;
  // owners can be a count or detailed list depending on context
  owners?: number | Array<{ steamId: string; playtime_hours?: number }>;
  hasAdultContent: boolean;
}

// Compatibility: some APIs use `appid` and `playtime_hours` fields
export interface GameWithOwners extends Game {
  appid?: number;
  playtime_hours?: number;
  img_icon_url?: string;
  owners: Array<{ steamId: string; playtime_hours?: number }>;
  copies?: number;
  hiddenForSexContent?: boolean;
  genres?: string[];
}

export interface SteamUser {
  steamId: string;
  personaName: string;
  avatar: string;
  accountCreated: string; // ISO format
  stats: { hours: number; games: number; uniques: number };
}

export interface ComparedUser {
  steamId: string;
  personaName?: string;
  avatarUrl?: string;
  createdAt?: number | null;
  games?: any[]; // raw owned games from Steam conector
  totalHours?: number;
  editing?: boolean;
}

// Library-specific state
export interface LibraryState {
  query: string;
  tags: string[];
  avatars: string[];
  includeAdult: boolean;
  viewMode: 'grid' | 'library';
  loading: boolean;
  error: string | null;
  games: Game[];
}

export interface CompareState {
  users: SteamUser[];
  loading: boolean;
  error: string | null;
}
