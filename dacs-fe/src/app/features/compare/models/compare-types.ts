/**
 * Tipos compartidos para la funcionalidad de comparación de bibliotecas
 */

export interface CompareFilters {
  onlyOwned?: boolean;
  coOp?: boolean;
  sortBy?: 'hours_sum'|'name'|'metacritic'|'last_played';
  limit?: number;
}

export interface CompareRequest {
  users: string[];
  filters?: CompareFilters;
}

export interface CompareResponse {
  summary: { 
    users: number; 
    overlap_count: number; 
    jaccard: number;
    user1: { nickname: string; totalGames: number; totalPlaytime: number };
    user2: { nickname: string; totalGames: number; totalPlaytime: number };
    category: string;
  };
  overlap: Array<{
    appId: string;
    name: string;
    hoursByUser: Record<string, number>;
    coOp: boolean;
    lastPlayed?: string;
    metacritic?: number;
    iconUrl?: string;
    isFree?: boolean;
  }>;
  onlyUserA?: any[];
  onlyUserB?: any[];
}
