import { Directive, ElementRef, HostListener, Input, OnDestroy, Renderer2 } from '@angular/core';

@Directive({
  selector: '[appTooltipOverlay]',
  standalone: true
})
export class TooltipOverlayDirective implements OnDestroy {
  @Input('appTooltipOverlay') text: string | undefined;

  private tooltipEl: HTMLElement | null = null;
  private resizeHandler = () => this.positionTooltip();
  private scrollHandler = () => this.positionTooltip();

  constructor(private host: ElementRef<HTMLElement>, private renderer: Renderer2) {}

  @HostListener('mouseenter')
  onEnter(): void {
    if (!this.text) return;
    this.showTooltip();
  }

  @HostListener('mouseleave')
  onLeave(): void {
    this.hideTooltip();
  }

  private createTooltip(): void {
    if (this.tooltipEl) return;
    const el = this.renderer.createElement('div') as HTMLElement;
    el.className = 'dacs-tooltip-overlay';
    this.renderer.setStyle(el, 'position', 'fixed');
    this.renderer.setStyle(el, 'top', '0px');
    this.renderer.setStyle(el, 'left', '0px');
    this.renderer.setStyle(el, 'transform', 'translate(-50%, -8px)');
    this.renderer.setStyle(el, 'background', 'linear-gradient(135deg, rgba(27,40,56,0.98) 0%, rgba(20,30,48,0.98) 100%)');
    this.renderer.setStyle(el, 'color', '#fff');
    this.renderer.setStyle(el, 'padding', '0.6rem 0.9rem');
    this.renderer.setStyle(el, 'border-radius', '8px');
    this.renderer.setStyle(el, 'font-size', '0.95rem');
    this.renderer.setStyle(el, 'font-weight', '600');
    this.renderer.setStyle(el, 'box-shadow', '0 12px 32px rgba(0,0,0,0.8), 0 0 0 2px rgba(251,191,36,0.25)');
    this.renderer.setStyle(el, 'border', '1px solid rgba(251,191,36,0.6)');
    this.renderer.setStyle(el, 'z-index', '150000');
    this.renderer.setStyle(el, 'max-width', '420px');
    this.renderer.setStyle(el, 'white-space', 'pre-wrap');
    this.renderer.setStyle(el, 'text-align', 'center');
    this.renderer.setStyle(el, 'pointer-events', 'none');
    this.renderer.setStyle(el, 'opacity', '0');
    this.renderer.setStyle(el, 'transition', 'opacity 0.12s ease-out');

    this.renderer.appendChild(document.body, el);
    this.tooltipEl = el;
  }

  private showTooltip(): void {
    this.createTooltip();
    if (!this.tooltipEl) return;
    this.tooltipEl.textContent = this.text || '';
    this.positionTooltip();
    // small delay to allow positioning before fade-in
    requestAnimationFrame(() => {
      this.renderer.setStyle(this.tooltipEl!, 'opacity', '1');
    });
    window.addEventListener('resize', this.resizeHandler);
    window.addEventListener('scroll', this.scrollHandler, true);
  }

  private hideTooltip(): void {
    if (!this.tooltipEl) return;
    this.renderer.setStyle(this.tooltipEl, 'opacity', '0');
    // remove after transition
    setTimeout(() => {
      if (this.tooltipEl) {
        try { this.renderer.removeChild(document.body, this.tooltipEl); } catch { }
        this.tooltipEl = null;
      }
    }, 160);
    window.removeEventListener('resize', this.resizeHandler);
    window.removeEventListener('scroll', this.scrollHandler, true);
  }

  private positionTooltip(): void {
    if (!this.tooltipEl) return;
    const hostRect = this.host.nativeElement.getBoundingClientRect();
    const ttRect = this.tooltipEl.getBoundingClientRect();

    // position tooltip above the host element, horizontally centered
    let top = hostRect.top - ttRect.height - 8;
    // if not enough space above, position below
    if (top < 8) {
      top = hostRect.bottom + 8;
      this.renderer.setStyle(this.tooltipEl, 'transform', 'translateX(-50%)');
    } else {
      this.renderer.setStyle(this.tooltipEl, 'transform', 'translateX(-50%)');
    }

    let left = hostRect.left + hostRect.width / 2;

    // ensure tooltip stays within viewport horizontally
    const padding = 8;
    const minLeft = padding + ttRect.width / 2;
    const maxLeft = window.innerWidth - padding - ttRect.width / 2;
    if (left < minLeft) left = minLeft;
    if (left > maxLeft) left = maxLeft;

    this.renderer.setStyle(this.tooltipEl, 'top', `${Math.round(top)}px`);
    this.renderer.setStyle(this.tooltipEl, 'left', `${Math.round(left)}px`);
  }

  ngOnDestroy(): void {
    this.hideTooltip();
  }
}
