import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { CompareResponse } from '../../models/compare-types';

@Component({
  selector: 'app-results-table',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatTabsModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
    MatButtonModule
  ],
  template: `
    <mat-card class="results-card" *ngIf="data()">
      <mat-card-header>
        <mat-card-title>
          <mat-icon>table_chart</mat-icon>
          Resultados Detallados
        </mat-card-title>
        <mat-card-subtitle>
          Comparación detallada de bibliotecas de juegos
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <mat-tab-group class="results-tabs" animationDuration="200ms">
          <!-- Tab: Juegos en Común -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon>library_books</mat-icon>
              Juegos en Común ({{ data()!.overlap.length }})
            </ng-template>
            
            <div class="tab-content">
              <div class="table-container" *ngIf="data()!.overlap.length > 0; else noCommonGames">
                <table mat-table [dataSource]="data()!.overlap" class="games-table">
                  <!-- Game Column -->
                  <ng-container matColumnDef="game">
                    <th mat-header-cell *matHeaderCellDef>Juego</th>
                    <td mat-cell *matCellDef="let game" class="game-cell">
                      <div class="game-info">
                        <img *ngIf="game.iconUrl" 
                             [src]="game.iconUrl" 
                             [alt]="game.name"
                             class="game-icon"
                             (error)="onImageError($event)">
                        <div class="game-details">
                          <div class="game-name">{{ game.name }}</div>
                          <div class="game-metadata">
                            <mat-chip *ngIf="game.isFree" color="accent" class="free-chip">
                              <mat-icon>monetization_off</mat-icon>
                              Gratuito
                            </mat-chip>
                            <mat-chip *ngIf="game.coOp" color="primary" class="coop-chip">
                              <mat-icon>group</mat-icon>
                              Co-op
                            </mat-chip>
                            <span *ngIf="game.metacritic" class="metacritic-score">
                              <mat-icon>star</mat-icon>
                              {{ game.metacritic }}
                            </span>
                          </div>
                        </div>
                      </div>
                    </td>
                  </ng-container>

                  <!-- Hours Comparison Column -->
                  <ng-container matColumnDef="hours">
                    <th mat-header-cell *matHeaderCellDef>Tiempo de Juego</th>
                    <td mat-cell *matCellDef="let game" class="hours-cell">
                      <div class="hours-comparison">
                        <div class="user-hours" *ngFor="let user of getUsers(game.hoursByUser)">
                          <span class="user-label">{{ user.label }}:</span>
                          <span class="hours-value">{{ formatPlaytime(user.hours) }}</span>
                          <mat-progress-bar 
                            *ngIf="user.hours > 0"
                            mode="determinate" 
                            [value]="getRelativeHours(game.hoursByUser, user.hours)"
                            class="hours-bar">
                          </mat-progress-bar>
                        </div>
                      </div>
                    </td>
                  </ng-container>

                  <!-- Last Played Column -->
                  <ng-container matColumnDef="lastPlayed">
                    <th mat-header-cell *matHeaderCellDef>Última Vez Jugado</th>
                    <td mat-cell *matCellDef="let game">
                      <span *ngIf="game.lastPlayed; else neverPlayed" class="last-played">
                        {{ formatDate(game.lastPlayed) }}
                      </span>
                      <ng-template #neverPlayed>
                        <span class="never-played">Nunca</span>
                      </ng-template>
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="commonColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: commonColumns;" class="game-row"></tr>
                </table>
              </div>

              <ng-template #noCommonGames>
                <div class="empty-state">
                  <mat-icon>sentiment_dissatisfied</mat-icon>
                  <h3>No hay juegos en común</h3>
                  <p>Los usuarios no comparten ningún juego en sus bibliotecas.</p>
                </div>
              </ng-template>
            </div>
          </mat-tab>

          <!-- Tab: Solo Usuario A -->
          <mat-tab *ngIf="data()?.onlyUserA && data()!.onlyUserA!.length > 0">
            <ng-template mat-tab-label>
              <mat-icon>person</mat-icon>
              Solo {{ getUserName(1) }} ({{ data()!.onlyUserA!.length }})
            </ng-template>
            
            <div class="tab-content">
              <div class="table-container">
                <table mat-table [dataSource]="data()!.onlyUserA!" class="games-table">
                  <ng-container matColumnDef="game">
                    <th mat-header-cell *matHeaderCellDef>Juego</th>
                    <td mat-cell *matCellDef="let game" class="game-cell">
                      <div class="game-info">
                        <img *ngIf="game.iconUrl" 
                             [src]="game.iconUrl" 
                             [alt]="game.name"
                             class="game-icon"
                             (error)="onImageError($event)">
                        <div class="game-details">
                          <div class="game-name">{{ game.name }}</div>
                          <div class="game-metadata">
                            <mat-chip *ngIf="game.isFree" color="accent">Gratuito</mat-chip>
                            <mat-chip *ngIf="game.coOp" color="primary">Co-op</mat-chip>
                          </div>
                        </div>
                      </div>
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="hours">
                    <th mat-header-cell *matHeaderCellDef>Tiempo de Juego</th>
                    <td mat-cell *matCellDef="let game">
                      {{ formatPlaytime(getTotalHours(game.hoursByUser)) }}
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="uniqueColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: uniqueColumns;"></tr>
                </table>
              </div>
            </div>
          </mat-tab>

          <!-- Tab: Solo Usuario B -->
          <mat-tab *ngIf="data()?.onlyUserB && data()!.onlyUserB!.length > 0">
            <ng-template mat-tab-label>
              <mat-icon>person</mat-icon>
              Solo {{ getUserName(2) }} ({{ data()!.onlyUserB!.length }})
            </ng-template>
            
            <div class="tab-content">
              <div class="table-container">
                <table mat-table [dataSource]="data()!.onlyUserB!" class="games-table">
                  <ng-container matColumnDef="game">
                    <th mat-header-cell *matHeaderCellDef>Juego</th>
                    <td mat-cell *matCellDef="let game" class="game-cell">
                      <div class="game-info">
                        <img *ngIf="game.iconUrl" 
                             [src]="game.iconUrl" 
                             [alt]="game.name"
                             class="game-icon"
                             (error)="onImageError($event)">
                        <div class="game-details">
                          <div class="game-name">{{ game.name }}</div>
                          <div class="game-metadata">
                            <mat-chip *ngIf="game.isFree" color="accent">Gratuito</mat-chip>
                            <mat-chip *ngIf="game.coOp" color="primary">Co-op</mat-chip>
                          </div>
                        </div>
                      </div>
                    </td>
                  </ng-container>

                  <ng-container matColumnDef="hours">
                    <th mat-header-cell *matHeaderCellDef>Tiempo de Juego</th>
                    <td mat-cell *matCellDef="let game">
                      {{ formatPlaytime(getTotalHours(game.hoursByUser)) }}
                    </td>
                  </ng-container>

                  <tr mat-header-row *matHeaderRowDef="uniqueColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: uniqueColumns;"></tr>
                </table>
              </div>
            </div>
          </mat-tab>
        </mat-tab-group>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .results-card {
      margin-bottom: 24px;
      background: linear-gradient(135deg, #FFFFFF 0%, #F8FAFC 100%);
      border: 1px solid #E5E7EB;
      border-radius: 20px;
      box-shadow: 0 8px 25px -5px rgba(139, 92, 246, 0.2), 0 8px 10px -5px rgba(139, 92, 246, 0.04);
      overflow: hidden;
      transition: all 0.3s ease;
    }

    .results-card:hover {
      box-shadow: 0 20px 40px -10px rgba(139, 92, 246, 0.3), 0 20px 20px -10px rgba(139, 92, 246, 0.08);
      transform: translateY(-4px);
    }

    mat-card-header {
      background: linear-gradient(135deg, #7C3AED 0%, #6366F1 100%);
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
      background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="dots" width="20" height="20" patternUnits="userSpaceOnUse"><circle cx="10" cy="10" r="1" fill="white" opacity="0.1"/></pattern></defs><rect width="100" height="100" fill="url(%23dots)"/></svg>');
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

    mat-card-header mat-icon {
      margin-right: 12px;
      font-size: 28px;
      width: 28px;
      height: 28px;
      color: white;
    }

    mat-card-content {
      padding: 0;
    }

    .results-tabs {
      margin-top: 0;
    }

    .results-tabs .mat-mdc-tab-header {
      background: #F8FAFC;
      border-bottom: 1px solid #E5E7EB;
    }

    .results-tabs .mat-mdc-tab-label {
      color: #6B7280;
      font-weight: 500;
      padding: 16px 24px;
      transition: all 0.3s ease;
    }

    .results-tabs .mat-mdc-tab-label:hover {
      background: rgba(139, 92, 246, 0.1);
      color: #8B5CF6;
    }

    .results-tabs .mat-mdc-tab-label-active {
      color: #8B5CF6 !important;
      background: rgba(139, 92, 246, 0.1);
    }

    .results-tabs .mdc-tab-indicator__content--underline {
      border-color: #8B5CF6 !important;
      border-width: 3px !important;
    }

    .tab-content {
      padding: 24px;
      min-height: 400px;
    }

    .table-container {
      overflow-x: auto;
      max-height: 600px;
      overflow-y: auto;
      border-radius: 12px;
      border: 1px solid #E5E7EB;
      background: white;
      box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
    }

    .games-table {
      width: 100%;
      min-width: 600px;
    }

    .games-table .mat-mdc-header-row {
      background: linear-gradient(135deg, #8B5CF6, #7C3AED);
      color: white;
    }

    .games-table .mat-mdc-header-cell {
      color: white !important;
      font-weight: 600;
      border-bottom: none;
      padding: 16px 12px;
    }

    .game-cell {
      padding: 12px 16px;
      border-bottom: 1px solid #F3F4F6;
    }

    .game-info {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .game-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      flex-shrink: 0;
      border: 2px solid #E5E7EB;
      transition: all 0.3s ease;
    }

    .game-icon:hover {
      border-color: #8B5CF6;
      transform: scale(1.1);
    }

    .game-details {
      flex: 1;
      min-width: 0;
    }

    .game-name {
      font-weight: 600;
      color: #1F2937;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      font-size: 0.95rem;
    }

    .game-metadata {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 6px;
      flex-wrap: wrap;
    }

    .free-chip,
    .coop-chip {
      font-size: 0.7rem;
      height: 22px;
      border-radius: 12px;
      font-weight: 500;
    }

    .free-chip {
      background: linear-gradient(135deg, #10B981, #059669) !important;
      color: white !important;
    }

    .coop-chip {
      background: linear-gradient(135deg, #8B5CF6, #7C3AED) !important;
      color: white !important;
    }

    .free-chip mat-icon,
    .coop-chip mat-icon {
      font-size: 12px;
      width: 12px;
      height: 12px;
    }

    .metacritic-score {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 0.8rem;
      color: #F59E0B;
      background: #FEF3C7;
      padding: 2px 8px;
      border-radius: 10px;
      font-weight: 600;
    }

    .metacritic-score mat-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
      color: #D97706;
    }

    .hours-cell {
      min-width: 200px;
    }

    .hours-comparison {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .user-hours {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 6px 10px;
      background: #F8FAFC;
      border-radius: 8px;
      transition: all 0.3s ease;
    }

    .user-hours:hover {
      background: #EDE9FE;
      transform: translateX(4px);
    }

    .user-label {
      font-size: 0.8rem;
      color: #6B7280;
      min-width: 60px;
      font-weight: 500;
    }

    .hours-value {
      font-weight: 600;
      min-width: 60px;
      color: #8B5CF6;
      font-size: 0.9rem;
    }

    .hours-bar {
      flex: 1;
      height: 6px;
      border-radius: 3px;
      background: #E5E7EB;
      overflow: hidden;
    }

    .hours-bar .mat-mdc-progress-bar-fill::after {
      background: linear-gradient(90deg, #8B5CF6, #C084FC) !important;
    }

    .last-played {
      font-size: 0.875rem;
      color: #6B7280;
      padding: 4px 8px;
      background: #F1F5F9;
      border-radius: 6px;
    }

    .never-played {
      font-size: 0.875rem;
      color: #9CA3AF;
      font-style: italic;
      padding: 4px 8px;
      background: #F9FAFB;
      border-radius: 6px;
    }

    .game-row {
      transition: all 0.3s ease;
    }

    .game-row:hover {
      background: linear-gradient(135deg, #F8FAFC, #EDE9FE) !important;
      transform: translateX(4px);
      box-shadow: 4px 0 0 #8B5CF6;
    }

    .empty-state {
      text-align: center;
      padding: 60px 20px;
      color: #6B7280;
      background: linear-gradient(135deg, #F8FAFC, #E2E8F0);
      border-radius: 16px;
      margin: 20px;
    }

    .empty-state mat-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #C084FC;
      margin-bottom: 16px;
      animation: bounce 2s infinite;
    }

    @keyframes bounce {
      0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
      40% { transform: translateY(-10px); }
      60% { transform: translateY(-5px); }
    }

    .empty-state h3 {
      margin: 16px 0 8px 0;
      color: #374151;
      font-weight: 600;
      font-size: 1.25rem;
    }

    .empty-state p {
      margin: 0;
      font-size: 1rem;
      color: #6B7280;
    }

    @media (max-width: 768px) {
      .games-table {
        min-width: 500px;
      }

      .game-info {
        gap: 8px;
      }

      .game-icon {
        width: 32px;
        height: 32px;
      }

      .hours-comparison {
        gap: 6px;
      }

      .user-hours {
        gap: 6px;
        font-size: 0.8rem;
        padding: 4px 8px;
      }

      .tab-content {
        padding: 16px;
      }

      .empty-state {
        padding: 40px 16px;
      }

      .empty-state mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
      }
    }

    /* Animaciones suaves */
    * {
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
  `]
})
export class ResultsTableComponent {
  data = input<CompareResponse | null>(null);

  commonColumns = ['game', 'hours', 'lastPlayed'];
  uniqueColumns = ['game', 'hours'];

  getUsers(hoursByUser: Record<string, number>) {
    return Object.entries(hoursByUser).map(([key, hours]) => ({
      label: key === 'user1' ? this.getUserName(1) : this.getUserName(2),
      hours
    }));
  }

  getUserName(userNumber: 1 | 2): string {
    const summary = this.data()?.summary;
    if (userNumber === 1) {
      return summary?.user1?.nickname || 'Usuario 1';
    }
    return summary?.user2?.nickname || 'Usuario 2';
  }

  getTotalHours(hoursByUser: Record<string, number>): number {
    return Object.values(hoursByUser).reduce((sum, hours) => sum + hours, 0);
  }

  getRelativeHours(hoursByUser: Record<string, number>, currentHours: number): number {
    const maxHours = Math.max(...Object.values(hoursByUser));
    return maxHours > 0 ? (currentHours / maxHours) * 100 : 0;
  }

  formatPlaytime(minutes: number): string {
    if (!minutes || minutes === 0) return '0 min';
    
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    
    if (hours === 0) return `${mins} min`;
    if (mins === 0) return `${hours}h`;
    return `${hours}h ${mins}m`;
  }

  formatDate(dateString: string): string {
    try {
      const date = new Date(dateString);
      const now = new Date();
      const diffTime = Math.abs(now.getTime() - date.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      
      if (diffDays === 1) return 'Ayer';
      if (diffDays < 7) return `Hace ${diffDays} días`;
      if (diffDays < 30) return `Hace ${Math.ceil(diffDays / 7)} semanas`;
      if (diffDays < 365) return `Hace ${Math.ceil(diffDays / 30)} meses`;
      return `Hace ${Math.ceil(diffDays / 365)} años`;
    } catch {
      return 'Fecha desconocida';
    }
  }

  onImageError(event: any) {
    event.target.style.display = 'none';
  }
}