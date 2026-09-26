import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CompareSlot, Game, JuegoComparacionDto, BibliotecaComparacionDto } from '../../models/compare.models';

@Component({
  selector: 'library-grid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './library-grid.component.html',
  styleUrls: ['./library-grid.component.css']
})
export class LibraryGridComponent {
  @Input() slots: CompareSlot[] = [];
  @Input() comparison: BibliotecaComparacionDto | null = null;
  @Input() games: JuegoComparacionDto[] = [];
  @Input() showLibraryFormat = false;

  @Output() hover = new EventEmitter<Game | JuegoComparacionDto | null>();

  get allGames(): (Game | JuegoComparacionDto)[] {
    if (this.comparison && this.comparison.juegosComunes && this.comparison.juegosComunes.length > 0) {
      return this.comparison.juegosComunes;
    }

    if (this.slots && this.slots.length > 0) {
      const map = new Map<number, Game>();
      for (const s of this.slots) {
        const games = s.library?.games || [];
        for (const g of games) {
          if (g && g.appid) map.set(g.appid, g);
        }
      }
      return Array.from(map.values());
    }

    return this.games || [];
  }
}
