import { AsyncPipe, DecimalPipe, CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { SteamApiService } from '../../../../core/models/steam/steam-api-response';

@Component({
  selector: 'app-library-list',
  standalone: true,
  imports: [CommonModule, AsyncPipe, DecimalPipe],
  templateUrl: './library-list.html',
  styleUrl: './library-list.css'
})
export class LibraryList {
  api = inject(SteamApiService);
}
