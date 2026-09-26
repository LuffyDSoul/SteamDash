import { Component, OnInit, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AddUserSlotComponent } from './components/add-user-slot/add-user-slot.component';
import { UserCompareCardComponent } from './components/user-compare-card/user-compare-card.component';
import { LibraryGridComponent } from './components/library-grid/library-grid.component';
import { GameHoverPanelComponent } from './components/game-hover-panel/game-hover-panel.component';
import { LibraryCompareService } from './services/library-compare.service';
import { ComparedUser, GameWithOwners, Game } from './models/library.models';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'library-compare',
  standalone: true,
  imports: [CommonModule, FormsModule, AddUserSlotComponent, UserCompareCardComponent, LibraryGridComponent, GameHoverPanelComponent],
  templateUrl: './library-compare.component.html',
  styleUrls: ['./library-compare.component.css']
})
export class LibraryCompareComponent implements OnInit {
  protected users = signal<ComparedUser[]>([]);
  protected viewMode = signal<'compare' | 'library'>('compare');
  protected combinedGames = signal<GameWithOwners[]>([]);
  protected hoveredGame = signal<GameWithOwners | null>(null);
  protected filterName = signal<string>('');
  protected filterTags = signal<string>('');
  protected showSexContent = signal<boolean>(false);

  constructor(private svc: LibraryCompareService) {}

  ngOnInit(): void {
    this.svc.getInitialComparedUsers().subscribe((u: ComparedUser[]) => this.users.set(u));

    // recompute combined games when users change
    effect(() => {
      const u = this.users();
      this.combinedGames.set(this.buildCombined(u));
    });
  }

  protected toggleView() {
    this.viewMode.update(v => (v === 'compare' ? 'library' : 'compare'));
  }

  protected requestAdd() {
    if (this.users().length >= 6) return;
    const newSlot: ComparedUser = { steamId: '', personaName: '', avatarUrl: '', games: [], editing: true };
    this.users.update(cur => [newSlot, ...cur]);
  }

  // Called when user submits a steamId from the child card
  protected addOrUpdateUser(inputId: string, index = 0) {
    if (!inputId) {
      console.warn('addOrUpdateUser: inputId is empty');
      return;
    }
    
    console.log('addOrUpdateUser called with steamId:', inputId, 'index:', index);
    
    // If slot exists with empty steamId, update it; otherwise replace based on steamId
    const idx = this.users().findIndex(u => !u.steamId);
    const targetIndex = idx >= 0 ? idx : index;

    console.log('Target index for user:', targetIndex);

    // Fetch profile and library in parallel using forkJoin
    forkJoin({
      profile: this.svc.getUserProfile(inputId),
      library: this.svc.getUserLibrary(inputId)
    }).subscribe({
      next: ({ profile, library }) => {
        console.log('Profile received:', profile);
        console.log('Library received:', library);
        
        const lib = library || [];
        const totalHours = lib.reduce((s: number, g: any) => s + (g.playtime_hours || 0), 0);
        
        const user: ComparedUser = {
          steamId: inputId,
          personaName: profile?.personaName || inputId,
          avatarUrl: profile?.avatarUrl || '',
          createdAt: profile?.createdAt,
          games: lib,
          totalHours: totalHours,
          editing: false
        };
        
        console.log('User object created:', user);

        this.users.update(cur => {
          const next = [...cur];
          if (targetIndex >= 0 && targetIndex < next.length && !next[targetIndex].steamId) {
            console.log('Updating existing slot at index:', targetIndex);
            next[targetIndex] = user;
          } else {
            console.log('Adding new user at beginning');
            next.unshift(user);
          }
          // ensure max 6
          const result = next.slice(0, 6);
          console.log('Updated users array:', result);
          return result;
        });
      },
      error: (err) => {
        console.error('Error fetching user data:', err);
        alert('Error al obtener datos del usuario. Verifica el Steam ID.');
      }
    });
  }

  protected removeUser(steamId: string) {
    this.users.update(cur => cur.filter(u => u.steamId !== steamId));
  }

  protected buildCombined(users: ComparedUser[]): GameWithOwners[] {
    const map = new Map<number, GameWithOwners>();
    users.forEach(u => {
      (u.games || []).forEach((g: any) => {
        const key = (g.appid ?? g.appId) as number | undefined;
        if (!key) {
          // skip invalid/unknown appids
          return;
        }

        const existing = map.get(key);
        if (existing) {
          existing.owners = existing.owners || [];
          existing.owners.push({ steamId: u.steamId, playtime_hours: g.playtime_hours });
          existing.copies = (existing.owners && existing.owners.length) || 0;
        } else {
          map.set(key, {
            ...g,
            appid: key,
            owners: [{ steamId: u.steamId, playtime_hours: g.playtime_hours }],
            copies: 1
          });
        }
      });
    });
    return Array.from(map.values()).sort((a, b) => ( (b.copies ?? 0) - (a.copies ?? 0) ) || ( (b.playtime_hours ?? 0) - (a.playtime_hours ?? 0) ));
  }

  protected onHover(game: GameWithOwners | null) {
    this.hoveredGame.set(game);
  }

  // Filtering in template
  protected filteredGames(): GameWithOwners[] {
    const name = (this.filterName() || '').toLowerCase();
    const tags = (this.filterTags() || '').toLowerCase().split(',').map(s => s.trim()).filter(Boolean);
    return this.combinedGames().filter(g => {
      if (!this.showSexContent() && g.hiddenForSexContent) return false;
      if (name && !g.name?.toLowerCase().includes(name)) return false;
      if (tags.length) {
        const gs = (g.genres || []).map((x: string) => x.toLowerCase());
        if (!tags.every(t => gs.includes(t))) return false;
      }
      return true;
    });
  }
}
