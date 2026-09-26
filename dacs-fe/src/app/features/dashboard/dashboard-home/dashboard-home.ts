import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { SteamApiService } from '../../../core/models/steam/steam-api-response';
import { LibraryList } from '../widgets/library-list/library-list';
import { NewsFeed } from '../widgets/news-feed/news-feed';
import { Achievements } from '../widgets/achievements/achievements';
import { PlaytimeChart } from '../widgets/playtime-chart/playtime-chart';


@Component({
  selector: 'app-dashboard-home',
  imports: [CommonModule, ReactiveFormsModule, LibraryList, NewsFeed, Achievements, PlaytimeChart],
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.css'
})
export class DashboardHome {
  private api = inject(SteamApiService);
  search = new FormControl<string>('', { nonNullable: true });
  genre  = new FormControl<string>('all', { nonNullable: true });

  constructor() {
    this.search.valueChanges.subscribe(v => this.api.setQuery(v));
    this.genre.valueChanges.subscribe(v => this.api.setGenre(v));
  }
}
