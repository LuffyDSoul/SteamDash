import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UiBlockService {
  isBlocked = signal<boolean>(false);

  block() {
    this.isBlocked.set(true);
  }

  unblock() {
    this.isBlocked.set(false);
  }
}
