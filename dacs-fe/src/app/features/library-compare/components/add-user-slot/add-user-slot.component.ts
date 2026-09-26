import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'add-user-slot',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './add-user-slot.component.html',
  styleUrls: ['./add-user-slot.component.css']
})
export class AddUserSlotComponent {
  @Output() add = new EventEmitter<void>();
}
