import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GameWithOwners } from '../../models/library.models';

@Component({
  selector: 'game-hover-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-hover-panel.component.html',
  styleUrls: ['./game-hover-panel.component.css']
})
export class GameHoverPanelComponent {
  @Input() game?: GameWithOwners | null;
}
