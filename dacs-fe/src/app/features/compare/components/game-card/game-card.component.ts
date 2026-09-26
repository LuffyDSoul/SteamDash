import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Game } from '../../models/compare.models';

@Component({
  selector: 'game-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-card.component.html',
  styleUrls: ['./game-card.component.css']
})
export class GameCardComponent {
  @Input() game!: Game;
  @Input() showCopies: boolean = false;
  
  private imageAttempts = 0;

  onImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    
    // Intentar con alternativas en cascada
    if (this.imageAttempts === 0 && this.game.headerImage && imgElement.src !== this.game.headerImage) {
      // Primer intento fallido, probar con headerImage
      imgElement.src = this.game.headerImage;
      this.imageAttempts++;
    } else {
      // Ya probamos todas las opciones, mostrar placeholder
      imgElement.style.display = 'none';
      const wrapper = imgElement.parentElement;
      if (wrapper && !wrapper.querySelector('.game-image-placeholder')) {
        const placeholder = document.createElement('div');
        placeholder.className = 'game-image-placeholder';
        placeholder.innerHTML = `
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 10H3V8h18v8zM6 15h2v-2H6v2zm3.5 0h2v-2h-2v2zm3.5 0h2v-2h-2v2z"/>
          </svg>
          <span class="placeholder-text">Sin imagen</span>
        `;
        wrapper.insertBefore(placeholder, wrapper.firstChild);
      }
    }
  }

  /**
   * Abrir la tienda en una nueva pestaña de forma explícita.
   * Esto evita que otros manejadores de eventos intercepten y prevengan
   * la navegación por defecto del <a>.
   */
  openStore(event: MouseEvent, url: string) {
    // Evitar que el click burbujee y pueda ser interceptado por padres
    event.stopPropagation();
    try {
      // window.open ejecutado en el contexto de un click del usuario no debe ser bloqueado
      window.open(url, '_blank', 'noopener');
    } catch (e) {
      // Como fallback, asignar location.href para navegar en la misma pestaña
      window.location.href = url;
    }
  }
}
