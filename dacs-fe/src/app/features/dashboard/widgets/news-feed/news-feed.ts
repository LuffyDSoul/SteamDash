import { Component, inject } from '@angular/core';
import { SteamApiService } from '../../../../core/models/steam/steam-api-response';
import { AsyncPipe, CommonModule, DatePipe } from '@angular/common';

@Component({
  selector: 'app-news-feed',
  imports: [CommonModule, AsyncPipe, DatePipe],
  templateUrl: './news-feed.html',
  styleUrl: './news-feed.css'
})
export class NewsFeed {
  api = inject(SteamApiService);
}
