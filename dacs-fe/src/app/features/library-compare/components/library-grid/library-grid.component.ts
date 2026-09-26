import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameWithOwners } from '../../models/library.models';

@Component({
  selector: 'library-grid',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './library-grid.component.html',
  styleUrls: ['./library-grid.component.css']
})
export class LibraryGridComponent {
  @Input() games: GameWithOwners[] = [];
  @Output() hover = new EventEmitter<GameWithOwners | null>();
}
