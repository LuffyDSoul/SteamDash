import { CommonModule } from '@angular/common';
import { Component, DestroyRef, ElementRef, inject, ViewChild } from '@angular/core';
import { SteamApiService } from '../../../../core/models/steam/steam-api-response';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-playtime-chart',
  imports: [CommonModule],
  templateUrl: './playtime-chart.html',
  styleUrl: './playtime-chart.css'
})
export class PlaytimeChart {
@ViewChild('cvs', { static: true }) canvas!: ElementRef<HTMLCanvasElement>;
  private api = inject(SteamApiService);
  private destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.api.playtimeSeries$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(series => this.draw(series));
  }

  private draw(series: { label: string; value: number }[]) {
    const cvs = this.canvas.nativeElement;
    const ctx = cvs.getContext('2d')!;
    const W = cvs.width, H = cvs.height;

    // clear
    ctx.clearRect(0,0,W,H);

    // bg grid
    ctx.strokeStyle = 'rgba(148,163,184,0.12)';
    ctx.lineWidth = 1;
    for (let y=40; y<H-20; y+=40) {
      ctx.beginPath();
      ctx.moveTo(40, y); ctx.lineTo(W-10, y); ctx.stroke();
    }

    // bars
    const max = Math.max(1, ...series.map(s => s.value));
    const padL = 80, padR = 20, padB = 40, padT = 20;
    const innerW = W - padL - padR;
    const innerH = H - padT - padB;
    const barW = innerW / Math.max(1, series.length) * 0.6;

    series.forEach((s, i) => {
      const x = padL + i * (innerW / series.length) + (innerW/series.length - barW)/2;
      const h = (s.value / max) * innerH;
      const y = H - padB - h;

      // glow bar (accent)
      const grad = ctx.createLinearGradient(0, y, 0, y + h);
      grad.addColorStop(0, 'rgba(34,211,238,0.8)');
      grad.addColorStop(1, 'rgba(139,92,246,0.8)');
      ctx.fillStyle = grad;
      ctx.shadowBlur = 16; ctx.shadowColor = 'rgba(139,92,246,0.35)';
      ctx.fillRect(x, y, barW, h);
      ctx.shadowBlur = 0;

      // label
      ctx.fillStyle = '#94a3b8';
      ctx.font = '12px system-ui';
      ctx.textAlign = 'center';
      const lbl = s.label.length > 12 ? s.label.slice(0,10) + '…' : s.label;
      ctx.fillText(lbl, x + barW/2, H - 18);

      // value
      ctx.fillStyle = '#dbeafe';
      ctx.font = '11px system-ui';
      ctx.fillText(Math.round(s.value/60) + 'h', x + barW/2, y - 6);
    });

    // axes
    ctx.strokeStyle = 'rgba(148,163,184,0.2)';
    ctx.beginPath();
    ctx.moveTo(padL, padT); ctx.lineTo(padL, H - padB); ctx.lineTo(W - padR, H - padB); ctx.stroke();
  }
}
