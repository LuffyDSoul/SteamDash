import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ComparedUser } from '../../models/library.models';

@Component({
  selector: 'user-compare-card',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-compare-card.component.html',
  styleUrls: ['./user-compare-card.component.css']
})
export class UserCompareCardComponent {
  @Input() user!: ComparedUser;
  @Output() remove = new EventEmitter<string>();
  @Output() save = new EventEmitter<string>(); // emits steamId when saved/added

  editingLocal = false;
  inputId = '';

  ngOnInit(): void {
    if (this.user?.editing) {
      this.editingLocal = true;
      this.inputId = this.user.steamId || '';
    }
  }

  startEdit() {
    this.editingLocal = true;
    this.inputId = this.user?.steamId || '';
  }

  cancelEdit() {
    this.editingLocal = false;
  }

  confirmEdit() {
    const v = (this.inputId || '').trim();
    console.log('confirmEdit called with inputId:', v);
    if (v) {
      console.log('Emitting save event with steamId:', v);
      this.save.emit(v);
      this.editingLocal = false;
    } else {
      console.warn('confirmEdit: inputId is empty after trim');
    }
  }

  onRemove() {
    this.remove.emit(this.user.steamId);
  }
}
