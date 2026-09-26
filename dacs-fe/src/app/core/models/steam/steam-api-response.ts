import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, combineLatest, map, of, shareReplay, switchMap, timer } from 'rxjs';

export interface Game {
  appid: number;
  name: string;
  playtimeForever: number; // minutos
  lastTwoWeeks?: number;   // minutos
  img?: string;
  genres?: string[];
}
export interface NewsItem {
  appid: number;
  title: string;
  url: string;
  date: number; // epoch
}

@Injectable({ providedIn: 'root' })
export class SteamApiService {
  private query$ = new BehaviorSubject<string>('');
  private genre$ = new BehaviorSubject<string>('all');
  private refresh$ = new BehaviorSubject<void>(undefined);

  // Simulación / stub (reemplazá por llamadas reales a tu backend microservicios)
  private mockGames$ = timer(0, 30000).pipe( // “actualiza” cada 30s
    switchMap(() => of([
      { appid: 570, name: 'Dota 2', playtimeForever: 12345, lastTwoWeeks: 320, img: '', genres:['MOBA','Competitive'] },
      { appid: 730, name: 'Counter-Strike 2', playtimeForever: 9876, lastTwoWeeks: 210, img: '', genres:['FPS','Competitive'] },
      { appid: 1222670, name: 'Lethal Company', playtimeForever: 540, lastTwoWeeks: 180, img: '', genres:['Coop','Horror'] },
      { appid: 271590, name: 'GTA V', playtimeForever: 4321, lastTwoWeeks: 0, img: '', genres:['Open World','Action'] },
      { appid: 1245620, name: 'ELDEN RING', playtimeForever: 2500, lastTwoWeeks: 60, img: '', genres:['RPG','Soulslike'] },
    ] as Game[])),
    shareReplay(1)
  );

  games$ = combineLatest([this.mockGames$, this.query$, this.genre$]).pipe(
    map(([games, q, g]) => {
      const byQ = q.trim().toLowerCase();
      return games.filter(x => {
        const passQ = !byQ || x.name.toLowerCase().includes(byQ);
        const passG = g === 'all' || x.genres?.some(gg => gg.toLowerCase() === g.toLowerCase());
        return passQ && passG;
      });
    }),
    shareReplay(1)
  );

  // Playtime total para chart
  playtimeSeries$ = this.games$.pipe(
    map(gs => gs.map(g => ({ label: g.name, value: g.playtimeForever })))
  );

  // Noticias simuladas
  news$ = this.mockGames$.pipe(
    map(gs => gs.slice(0,3).map(g => ({
      appid: g.appid,
      title: `Patch notes de ${g.name}`,
      url: `https://store.steampowered.com/news/app/${g.appid}`,
      date: Date.now() - Math.floor(Math.random() * 86400000 * 3)
    } as NewsItem)))
  );

  // Expuestos para que la UI emita
  setQuery(q: string) { this.query$.next(q); }
  setGenre(g: string) { this.genre$.next(g); }
  refresh() { this.refresh$.next(); }
}
