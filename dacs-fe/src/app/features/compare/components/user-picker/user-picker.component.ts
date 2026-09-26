import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-user-picker',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Seleccionar Usuarios para Comparar</mat-card-title>
        <mat-card-subtitle>Ingresa Steam ID (17 dígitos) o vanity (usuario)</mat-card-subtitle>
      </mat-card-header>
      
      <mat-card-content>
        <form [formGroup]="userForm" (ngSubmit)="onSubmit()">
          <div class="user-inputs">
            <mat-form-field appearance="outline" class="user-field">
              <mat-label>Steam ID o vanity Usuario 1</mat-label>
              <input matInput 
                     formControlName="user1" 
                     placeholder="Ej: 76561198000000000 o luzier"
                     maxlength="64">
              <mat-icon matSuffix>person</mat-icon>
              <mat-error *ngIf="userForm.get('user1')?.hasError('required')">
                El Steam ID del usuario 1 es requerido
              </mat-error>
              <mat-error *ngIf="userForm.get('user1')?.hasError('pattern')">
                Formato de Steam ID inválido
              </mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="user-field">
              <mat-label>Steam ID o vanity Usuario 2</mat-label>
              <input matInput 
                     formControlName="user2" 
                     placeholder="Ej: 76561198000000001 o pepe"
                     maxlength="64">
              <mat-icon matSuffix>person</mat-icon>
              <mat-error *ngIf="userForm.get('user2')?.hasError('required')">
                El Steam ID del usuario 2 es requerido
              </mat-error>
              <mat-error *ngIf="userForm.get('user2')?.hasError('pattern')">
                Formato de Steam ID inválido
              </mat-error>
              <mat-error *ngIf="userForm.get('user2')?.hasError('sameUser')">
                Los Steam IDs deben ser diferentes
              </mat-error>
            </mat-form-field>
          </div>
        </form>
      </mat-card-content>

      <mat-card-actions align="end">
        <button mat-button 
                type="button" 
                (click)="onClear()"
                [disabled]="loading()">
          <mat-icon>clear</mat-icon>
          Limpiar
        </button>
        
        <button mat-raised-button 
                color="primary" 
                (click)="onSubmit()"
                [disabled]="userForm.invalid || loading()">
          <mat-icon>compare_arrows</mat-icon>
          {{ loading() ? 'Comparando...' : 'Comparar Bibliotecas' }}
        </button>
      </mat-card-actions>
    </mat-card>
  `,
  styles: [`
    .user-inputs {
      display: flex;
      gap: 16px;
      flex-wrap: wrap;
      margin: 16px 0;
    }

    .user-field {
      flex: 1;
      min-width: 250px;
    }

    mat-card {
      margin-bottom: 24px;
      background: linear-gradient(135deg, #FFFFFF 0%, #F8FAFC 100%);
      border: 1px solid #E5E7EB;
      border-radius: 16px;
      box-shadow: 0 4px 6px -1px rgba(139, 92, 246, 0.1), 0 2px 4px -1px rgba(139, 92, 246, 0.06);
      transition: all 0.3s ease;
    }

    mat-card:hover {
      box-shadow: 0 10px 25px -5px rgba(139, 92, 246, 0.2), 0 10px 10px -5px rgba(139, 92, 246, 0.04);
      transform: translateY(-2px);
    }

    mat-card-header {
      background: linear-gradient(135deg, #8B5CF6 0%, #7C3AED 100%);
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

    mat-card-actions {
      padding: 16px 24px;
      gap: 12px;
      background: #F8FAFC;
      border-radius: 0 0 16px 16px;
      border-top: 1px solid #E5E7EB;
    }

    .mat-mdc-form-field {
      background: white;
      border-radius: 8px;
      transition: all 0.3s ease;
    }

    .mat-mdc-form-field:hover {
      box-shadow: 0 2px 8px rgba(139, 92, 246, 0.1);
    }

    .mat-mdc-raised-button {
      border-radius: 8px;
      font-weight: 600;
      padding: 12px 24px;
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
    }

    mat-icon {
      color: #8B5CF6;
    }

    mat-card-header mat-icon {
      color: white;
    }

    @media (max-width: 768px) {
      .user-inputs {
        flex-direction: column;
        gap: 12px;
      }
      
      .user-field {
        min-width: unset;
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
export class UserPickerComponent {
  loading = input<boolean>(false);
  usersSelected = output<string[]>();

  userForm = new FormGroup({
    user1: new FormControl('', [
      Validators.required,
      this.steamIdOrVanityValidator
    ]),
    user2: new FormControl('', [
      Validators.required,
      this.steamIdOrVanityValidator
    ])
  }, [this.differentUsersValidator]);

  onSubmit() {
    if (this.userForm.valid) {
      const user1 = this.userForm.get('user1')?.value || '';
      const user2 = this.userForm.get('user2')?.value || '';
      this.usersSelected.emit([user1, user2]);
    }
  }

  onClear() {
    this.userForm.reset();
  }

  private steamIdOrVanityValidator(control: AbstractControl): ValidationErrors | null {
    const v = (control.value || '').toString().trim();
    if (!v) return null; // 'required' handles empties
    // steamid64: exactly 17 digits
    if (/^\d{17}$/.test(v)) return null;
    // vanity: allow letters, numbers, underscore, dash, dot, 2-64 chars
    if (/^[A-Za-z0-9_\-.]{2,64}$/.test(v)) return null;
    return { pattern: true };
  }

  private differentUsersValidator(control: AbstractControl): ValidationErrors | null {
    const formGroup = control as FormGroup;
    const user1 = formGroup.get('user1')?.value;
    const user2 = formGroup.get('user2')?.value;
    
    if (user1 && user2 && user1 === user2) {
      return { sameUser: true };
    }
    
    return null;
  }
}