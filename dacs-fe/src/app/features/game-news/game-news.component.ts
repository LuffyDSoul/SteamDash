import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { BffService } from '../../core/services/bff.service';

@Component({
  selector: 'app-game-news',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-news.component.html',
  styleUrls: ['./game-news.component.css']
})
export class GameNewsComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private bff = inject(BffService);

  appId: string | null = null;
  news: any[] = [];
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    this.appId = this.route.snapshot.paramMap.get('appId');
    if (this.appId) {
      this.loadNews(this.appId);
    } else {
      this.error = 'App ID no proporcionado';
    }
  }

  loadNews(appId: string) {
    this.loading = true;
    this.bff.getGameNews(appId, 10, 1000).subscribe({
      next: (resp) => {
        // Resp es ahora una lista de NewsViewDto: [{title, contents, url, dateFormatted, headerImageUrl}]
        this.news = Array.isArray(resp) ? resp : [];
        this.loading = false;
      },
      error: (err) => {
        this.error = err?.message ?? 'Error al obtener noticias';
        this.loading = false;
      }
    });
  }
}
