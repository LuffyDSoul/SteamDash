import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'add-user-slot',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-user-slot.component.html',
  styleUrls: ['./add-user-slot.component.css']
})
export class AddUserSlotComponent {
  steamId = '';
  showInput = false;
  
  @Output() addUser = new EventEmitter<string>();

  onToggle() {
    this.showInput = !this.showInput;
    if (!this.showInput) {
      this.steamId = '';
    }
  }

  onAdd() {
    const v = this.steamId.trim();
    if (v) {
      this.addUser.emit(v);
      this.steamId = '';
      this.showInput = false;
    }
  }

  onCancel() {
    this.steamId = '';
    this.showInput = false;
  }
}
