import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SteamUser } from '../../models/compare.models';

@Component({
  selector: 'user-compare-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-compare-card.component.html',
  styleUrls: ['./user-compare-card.component.css']
})
export class UserCompareCardComponent {
  @Input() user!: SteamUser;
  @Input() loading: boolean = false; // Nueva propiedad para saber si está cargando
  @Output() remove = new EventEmitter<string>();
  @Output() edit = new EventEmitter<{ oldId: string; newId: string }>();

  protected isEditing = signal<boolean>(false);
  protected editingSteamId = signal<string>('');

  onRemove() {
    if (this.loading) return; // No permitir eliminar durante comparación
    this.remove.emit(this.user.steamId);
  }

  onStartEdit() {
    this.isEditing.set(true);
    this.editingSteamId.set(this.user.steamId);
  }

  onCancelEdit() {
    this.isEditing.set(false);
    this.editingSteamId.set('');
  }

  onConfirmEdit() {
    const newId = this.editingSteamId().trim();
    if (newId && newId !== this.user.steamId) {
      this.edit.emit({ oldId: this.user.steamId, newId });
    }
    this.isEditing.set(false);
    this.editingSteamId.set('');
  }

  formatDate(isoDate: string): string {
    if (!isoDate) return 'N/A';
    try {
      const date = new Date(isoDate);
      return date.toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' });
    } catch {
      return 'N/A';
    }
  }

  formatNumber(num: number): string {
    return new Intl.NumberFormat('es-ES').format(num);
  }
}
