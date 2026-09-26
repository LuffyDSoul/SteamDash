import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, FormGroup } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CompareFilters } from '../../models/compare-types';

@Component({
  selector: 'app-filters-panel',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatCheckboxModule,
    MatSelectModule,
    MatSliderModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <mat-card class="filters-card">
      <mat-card-header>
        <mat-card-title>
          <mat-icon>tune</mat-icon>
          Filtros de Comparación
        </mat-card-title>
        <mat-card-subtitle>Personaliza los resultados de la comparación</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <form [formGroup]="filtersForm" class="filters-form">
          <!-- Checkbox Filters -->
          <div class="checkbox-group">
            <mat-checkbox formControlName="onlyOwned">
              <span class="checkbox-label">Solo juegos jugados</span>
              <span class="checkbox-description">Mostrar solo juegos con tiempo de juego</span>
            </mat-checkbox>

            <mat-checkbox formControlName="coOp">
              <span class="checkbox-label">Solo cooperativos</span>
              <span class="checkbox-description">Mostrar solo juegos que soporten cooperativo</span>
            </mat-checkbox>
          </div>

          <!-- Sort Options -->
          <div class="sort-section">
            <mat-form-field appearance="outline" class="sort-field">
              <mat-label>Ordenar por</mat-label>
              <mat-select formControlName="sortBy">
                <mat-option value="hours_sum">Tiempo total jugado</mat-option>
                <mat-option value="name">Nombre del juego</mat-option>
                <mat-option value="metacritic">Puntuación Metacritic</mat-option>
                <mat-option value="last_played">Jugado recientemente</mat-option>
              </mat-select>
              <mat-icon matSuffix>sort</mat-icon>
            </mat-form-field>
          </div>

          <!-- Limit Section -->
          <div class="limit-section">
            <mat-form-field appearance="outline" class="limit-field">
              <mat-label>Límite de resultados</mat-label>
              <input matInput 
                     type="number" 
                     formControlName="limit"
                     min="1" 
                     max="1000"
                     placeholder="Ej: 50">
              <mat-icon matSuffix>format_list_numbered</mat-icon>
            </mat-form-field>
            
            <div class="limit-info">
              <small>
                <mat-icon class="info-icon">info</mat-icon>
                Deja vacío para mostrar todos los resultados
              </small>
            </div>
          </div>
        </form>
      </mat-card-content>

      <mat-card-actions align="end">
        <button mat-button 
                type="button" 
                (click)="resetFilters()"
                [disabled]="loading()">
          <mat-icon>refresh</mat-icon>
          Restablecer
        </button>
        
        <button mat-raised-button 
                color="primary" 
                (click)="applyFilters()"
                [disabled]="loading()">
          <mat-icon>filter_list</mat-icon>
          Aplicar Filtros
        </button>
      </mat-card-actions>
    </mat-card>
  `,
  styles: [`
    .filters-card {
      margin-bottom: 24px;
      background: linear-gradient(135deg, #FFFFFF 0%, #F8FAFC 100%);
      border: 1px solid #E5E7EB;
      border-radius: 16px;
      box-shadow: 0 4px 6px -1px rgba(139, 92, 246, 0.1), 0 2px 4px -1px rgba(139, 92, 246, 0.06);
      transition: all 0.3s ease;
    }

    .filters-card:hover {
      box-shadow: 0 10px 25px -5px rgba(139, 92, 246, 0.2), 0 10px 10px -5px rgba(139, 92, 246, 0.04);
      transform: translateY(-2px);
    }

    mat-card-header {
      background: linear-gradient(135deg, #C084FC 0%, #A855F7 100%);
      color: white;
      padding: 20px 24px;
      border-radius: 16px 16px 0 0;
      margin: -1px -1px 0 -1px;
    }

    mat-card-title {
      font-weight: 600;
      font-size: 1.25rem;
      margin: 0;
    }

    mat-card-subtitle {
      color: rgba(255, 255, 255, 0.9) !important;
      font-size: 0.875rem;
      margin-top: 4px;
    }

    mat-card-content {
      padding: 24px;
    }

    .filters-form {
      display: flex;
      flex-direction: column;
      gap: 24px;
      padding: 16px 0;
    }

    .checkbox-group {
      display: flex;
      flex-direction: column;
      gap: 16px;
      padding: 16px;
      background: linear-gradient(135deg, #F3F4F6 0%, #E5E7EB 100%);
      border-radius: 12px;
      border: 1px solid #D1D5DB;
    }

    .checkbox-label {
      font-weight: 600;
      display: block;
      color: #374151;
    }

    .checkbox-description {
      font-size: 0.875rem;
      color: #6B7280;
      display: block;
      margin-top: 4px;
    }

    .mat-mdc-checkbox .mdc-checkbox__native-control:enabled:checked~.mdc-checkbox__background {
      background-color: #8B5CF6 !important;
      border-color: #8B5CF6 !important;
    }

    .sort-section {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
      background: linear-gradient(135deg, #EDE9FE 0%, #DDD6FE 100%);
      border-radius: 12px;
      border: 1px solid #C4B5FD;
    }

    .sort-field,
    .limit-field {
      flex: 1;
      max-width: 300px;
      background: white;
      border-radius: 8px;
    }

    .limit-section {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding: 16px;
      background: linear-gradient(135deg, #F0F0F1 0%, #E0E7FF 100%);
      border-radius: 12px;
      border: 1px solid #C7D2FE;
    }

    .limit-info {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: rgba(255, 255, 255, 0.8);
      border-radius: 8px;
      color: #6366F1;
      font-size: 0.875rem;
    }

    .info-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      color: #8B5CF6;
    }

    mat-card-header mat-icon {
      margin-right: 8px;
      color: white;
    }

    mat-card-actions {
      padding: 16px 24px;
      gap: 12px;
      background: #F8FAFC;
      border-radius: 0 0 16px 16px;
      border-top: 1px solid #E5E7EB;
    }

    .mat-mdc-raised-button {
      border-radius: 8px;
      font-weight: 600;
      text-transform: none;
      transition: all 0.3s ease;
    }

    .mat-mdc-button {
      border-radius: 8px;
      font-weight: 500;
      text-transform: none;
      transition: all 0.3s ease;
    }

    .mat-mdc-button:hover {
      background-color: rgba(139, 92, 246, 0.1);
      transform: translateY(-1px);
    }

    mat-icon {
      color: #8B5CF6;
    }

    @media (max-width: 768px) {
      .sort-section {
        flex-direction: column;
        align-items: stretch;
      }
      
      .sort-field,
      .limit-field {
        max-width: none;
      }

      mat-card-content {
        padding: 16px;
      }

      mat-card-actions {
        padding: 12px 16px;
        flex-direction: column;
        gap: 8px;
      }
    }
  `]
})
export class FiltersPanelComponent {
  loading = input<boolean>(false);
  initialFilters = input<CompareFilters>({});
  filtersChanged = output<CompareFilters>();

  filtersForm = new FormGroup({
    onlyOwned: new FormControl(false),
    coOp: new FormControl(false),
    sortBy: new FormControl<'hours_sum' | 'name' | 'metacritic' | 'last_played'>('hours_sum'),
    limit: new FormControl<number | null>(null)
  });

  ngOnInit() {
    // Aplicar filtros iniciales si existen
    const initial = this.initialFilters();
    if (initial) {
      this.filtersForm.patchValue({
        onlyOwned: initial.onlyOwned || false,
        coOp: initial.coOp || false,
        sortBy: initial.sortBy || 'hours_sum',
        limit: initial.limit || null
      });
    }
  }

  applyFilters() {
    const formValue = this.filtersForm.value;
    const filters: CompareFilters = {
      onlyOwned: formValue.onlyOwned || false,
      coOp: formValue.coOp || false,
      sortBy: formValue.sortBy || 'hours_sum',
      limit: formValue.limit || undefined
    };
    
    this.filtersChanged.emit(filters);
  }

  resetFilters() {
    this.filtersForm.reset({
      onlyOwned: false,
      coOp: false,
      sortBy: 'hours_sum',
      limit: null
    });
    
    // Emitir filtros por defecto
    this.applyFilters();
  }
}