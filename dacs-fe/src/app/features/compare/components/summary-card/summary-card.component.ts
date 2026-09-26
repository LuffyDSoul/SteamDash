import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { CompareResponse } from '../../models/compare-types';

@Component({
  selector: 'app-summary-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatChipsModule
  ],
  template: `
    <mat-card class="summary-card" *ngIf="data()">
      <mat-card-header>
        <mat-card-title>
          <mat-icon>analytics</mat-icon>
          Resumen de Comparación
        </mat-card-title>
        <mat-card-subtitle>
          Análisis de similitud entre bibliotecas
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <!-- Métricas principales -->
        <div class="metrics-grid">
          <div class="metric-card">
            <div class="metric-icon">
              <mat-icon color="primary">library_books</mat-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ data()!.summary.overlap_count }}</div>
              <div class="metric-label">Juegos en común</div>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-icon">
              <mat-icon color="accent">trending_up</mat-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ (data()!.summary.jaccard * 100).toFixed(1) }}%</div>
              <div class="metric-label">Similitud (Jaccard)</div>
            </div>
          </div>

          <div class="metric-card">
            <div class="metric-icon">
              <mat-icon color="warn">group</mat-icon>
            </div>
            <div class="metric-content">
              <div class="metric-value">{{ data()!.summary.users }}</div>
              <div class="metric-label">Usuarios comparados</div>
            </div>
          </div>
        </div>

        <!-- Barra de similitud visual -->
        <div class="similarity-section">
          <div class="similarity-header">
            <span class="similarity-title">Índice de Similitud</span>
            <mat-chip [class]="getSimilarityClass()">
              {{ getSimilarityCategory() }}
            </mat-chip>
          </div>
          <mat-progress-bar 
            mode="determinate" 
            [value]="data()!.summary.jaccard * 100"
            [color]="getSimilarityColor()">
          </mat-progress-bar>
          <div class="similarity-description">
            {{ getSimilarityDescription() }}
          </div>
        </div>

        <!-- Información de usuarios -->
        <div class="users-section" *ngIf="hasUserInfo()">
          <h4 class="section-title">Información de Usuarios</h4>
          <div class="users-grid">
            <div class="user-info">
              <div class="user-header">
                <mat-icon>person</mat-icon>
                <span class="user-name">{{ data()!.summary.user1.nickname }}</span>
              </div>
              <div class="user-stats">
                <div class="user-stat">
                  <span class="stat-label">Total de juegos:</span>
                  <span class="stat-value">{{ data()!.summary.user1.totalGames }}</span>
                </div>
                <div class="user-stat">
                  <span class="stat-label">Tiempo total:</span>
                  <span class="stat-value">{{ formatPlaytime(data()!.summary.user1.totalPlaytime) }}</span>
                </div>
              </div>
            </div>

            <div class="user-info">
              <div class="user-header">
                <mat-icon>person</mat-icon>
                <span class="user-name">{{ data()!.summary.user2.nickname }}</span>
              </div>
              <div class="user-stats">
                <div class="user-stat">
                  <span class="stat-label">Total de juegos:</span>
                  <span class="stat-value">{{ data()!.summary.user2.totalGames }}</span>
                </div>
                <div class="user-stat">
                  <span class="stat-label">Tiempo total:</span>
                  <span class="stat-value">{{ formatPlaytime(data()!.summary.user2.totalPlaytime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Categoría de comparación -->
        <div class="category-section" *ngIf="data()!.summary.category">
          <div class="category-info">
            <mat-icon>category</mat-icon>
            <span>Categoría: <strong>{{ data()!.summary.category }}</strong></span>
          </div>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .summary-card {
      margin-bottom: 24px;
      background: linear-gradient(135deg, #FFFFFF 0%, #F8FAFC 100%);
      border: 1px solid #E5E7EB;
      border-radius: 20px;
      box-shadow: 0 8px 25px -5px rgba(139, 92, 246, 0.2), 0 8px 10px -5px rgba(139, 92, 246, 0.04);
      overflow: hidden;
      transition: all 0.3s ease;
    }

    .summary-card:hover {
      box-shadow: 0 20px 40px -10px rgba(139, 92, 246, 0.3), 0 20px 20px -10px rgba(139, 92, 246, 0.08);
      transform: translateY(-4px);
    }

    mat-card-header {
      background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%);
      color: white;
      padding: 24px;
      position: relative;
      overflow: hidden;
    }

    mat-card-header::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="white" opacity="0.1"/><circle cx="75" cy="75" r="1" fill="white" opacity="0.1"/><circle cx="50" cy="10" r="0.5" fill="white" opacity="0.1"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
      pointer-events: none;
    }

    mat-card-header * {
      position: relative;
      z-index: 1;
    }

    mat-card-title {
      font-weight: 700;
      font-size: 1.5rem;
      margin: 0;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
    }

    mat-card-subtitle {
      color: rgba(255, 255, 255, 0.9) !important;
      font-size: 0.95rem;
      margin-top: 8px;
      font-weight: 400;
    }

    mat-card-content {
      padding: 32px;
    }

    mat-card-header mat-icon {
      margin-right: 12px;
      font-size: 28px;
      width: 28px;
      height: 28px;
      color: white;
    }

    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 20px;
      margin-bottom: 32px;
    }

    .metric-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;
      background: linear-gradient(135deg, #F8FAFC 0%, #E2E8F0 100%);
      border: 1px solid #CBD5E1;
      border-radius: 16px;
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;
    }

    .metric-card::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 4px;
      height: 100%;
      background: linear-gradient(180deg, #8B5CF6, #C084FC);
    }

    .metric-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(139, 92, 246, 0.15);
      background: linear-gradient(135deg, #FFFFFF 0%, #F1F5F9 100%);
    }

    .metric-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 48px;
      background: linear-gradient(135deg, #8B5CF6, #A855F7);
      border-radius: 12px;
      color: white;
      box-shadow: 0 4px 8px rgba(139, 92, 246, 0.3);
    }

    .metric-content {
      flex: 1;
    }

    .metric-value {
      font-size: 1.8rem;
      font-weight: 700;
      line-height: 1;
      color: #1F2937;
      background: linear-gradient(135deg, #8B5CF6, #7C3AED);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .metric-label {
      font-size: 0.875rem;
      color: #6B7280;
      margin-top: 4px;
      font-weight: 500;
    }

    .similarity-section {
      margin-bottom: 32px;
      padding: 24px;
      background: linear-gradient(135deg, #F3F4F6 0%, #E5E7EB 100%);
      border-radius: 16px;
      border: 1px solid #D1D5DB;
    }

    .similarity-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .similarity-title {
      font-weight: 600;
      font-size: 1.1rem;
      color: #374151;
    }

    .similarity-description {
      font-size: 0.875rem;
      color: #6B7280;
      margin-top: 12px;
      text-align: center;
      padding: 12px;
      background: rgba(255, 255, 255, 0.8);
      border-radius: 8px;
      font-style: italic;
    }

    .users-section {
      margin-bottom: 24px;
    }

    .section-title {
      margin: 0 0 20px 0;
      font-weight: 600;
      color: #1F2937;
      font-size: 1.2rem;
      border-bottom: 2px solid #8B5CF6;
      padding-bottom: 8px;
      display: inline-block;
    }

    .users-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 20px;
    }

    .user-info {
      padding: 20px;
      background: linear-gradient(135deg, #FFFFFF 0%, #F8FAFC 100%);
      border: 2px solid #E5E7EB;
      border-radius: 16px;
      transition: all 0.3s ease;
      position: relative;
      overflow: hidden;
    }

    .user-info::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 4px;
      background: linear-gradient(90deg, #8B5CF6, #C084FC, #A855F7);
    }

    .user-info:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(139, 92, 246, 0.15);
      border-color: #8B5CF6;
    }

    .user-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid #E5E7EB;
    }

    .user-header mat-icon {
      color: #8B5CF6;
      font-size: 24px;
      width: 24px;
      height: 24px;
    }

    .user-name {
      font-weight: 600;
      font-size: 1.2rem;
      color: #1F2937;
    }

    .user-stats {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .user-stat {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
      background: #F8FAFC;
      border-radius: 8px;
      transition: all 0.2s ease;
    }

    .user-stat:hover {
      background: #EDE9FE;
    }

    .stat-label {
      color: #6B7280;
      font-size: 0.9rem;
      font-weight: 500;
    }

    .stat-value {
      font-weight: 600;
      color: #8B5CF6;
      font-size: 1rem;
    }

    .category-section {
      display: flex;
      justify-content: center;
      margin-top: 20px;
    }

    .category-info {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 24px;
      background: linear-gradient(135deg, #EDE9FE, #DDD6FE);
      border: 1px solid #C4B5FD;
      border-radius: 25px;
      color: #7C3AED;
      font-weight: 600;
      transition: all 0.3s ease;
    }

    .category-info:hover {
      transform: scale(1.05);
      box-shadow: 0 4px 12px rgba(139, 92, 246, 0.2);
    }

    .category-info mat-icon {
      color: #8B5CF6;
    }

    /* Clases para diferentes niveles de similitud */
    .similarity-low {
      background: linear-gradient(135deg, #FEF2F2, #FECACA) !important;
      color: #DC2626 !important;
      border-color: #FCA5A5 !important;
    }

    .similarity-medium {
      background: linear-gradient(135deg, #FFFBEB, #FED7AA) !important;
      color: #D97706 !important;
      border-color: #FDBA74 !important;
    }

    .similarity-high {
      background: linear-gradient(135deg, #F0FDF4, #BBF7D0) !important;
      color: #16A34A !important;
      border-color: #86EFAC !important;
    }

    @media (max-width: 768px) {
      .metrics-grid {
        grid-template-columns: 1fr;
        gap: 16px;
      }

      .users-grid {
        grid-template-columns: 1fr;
      }

      .similarity-header {
        flex-direction: column;
        align-items: stretch;
        gap: 12px;
      }

      mat-card-content {
        padding: 20px;
      }

      .metric-card {
        padding: 16px;
      }

      .similarity-section {
        padding: 16px;
      }
    }
  `]
})
export class SummaryCardComponent {
  data = input<CompareResponse | null>(null);

  hasUserInfo(): boolean {
    const summary = this.data()?.summary;
    return !!(summary?.user1 && summary?.user2);
  }

  getSimilarityClass(): string {
    const jaccard = this.data()?.summary.jaccard || 0;
    if (jaccard < 0.3) return 'similarity-low';
    if (jaccard < 0.6) return 'similarity-medium';
    return 'similarity-high';
  }

  getSimilarityColor(): 'primary' | 'accent' | 'warn' {
    const jaccard = this.data()?.summary.jaccard || 0;
    if (jaccard < 0.3) return 'warn';
    if (jaccard < 0.6) return 'accent';
    return 'primary';
  }

  getSimilarityCategory(): string {
    const jaccard = this.data()?.summary.jaccard || 0;
    if (jaccard < 0.2) return 'Muy Baja';
    if (jaccard < 0.4) return 'Baja';
    if (jaccard < 0.6) return 'Media';
    if (jaccard < 0.8) return 'Alta';
    return 'Muy Alta';
  }

  getSimilarityDescription(): string {
    const jaccard = this.data()?.summary.jaccard || 0;
    if (jaccard < 0.2) return 'Las bibliotecas tienen muy pocos juegos en común';
    if (jaccard < 0.4) return 'Las bibliotecas comparten algunos juegos, pero son bastante diferentes';
    if (jaccard < 0.6) return 'Las bibliotecas tienen una similitud moderada';
    if (jaccard < 0.8) return 'Las bibliotecas son bastante similares con muchos juegos en común';
    return 'Las bibliotecas son muy similares, casi idénticas';
  }

  formatPlaytime(minutes: number): string {
    if (!minutes || minutes === 0) return '0 min';
    
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    
    if (hours === 0) return `${mins} min`;
    if (mins === 0) return `${hours}h`;
    return `${hours}h ${mins}m`;
  }
}