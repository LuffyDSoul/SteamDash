import { Directive, ElementRef, OnDestroy, Renderer2, Input, OnChanges, SimpleChanges } from '@angular/core';

@Directive({
  selector: '[appAppendToBody]',
  standalone: true
})
export class AppendToBodyDirective implements OnDestroy, OnChanges {
  @Input('overlayTop') overlayTop?: string;
  @Input('overlayLeft') overlayLeft?: string;

  private appended = false;

  constructor(private host: ElementRef<HTMLElement>, private renderer: Renderer2) {
  }

  ngOnChanges(changes: SimpleChanges): void {
    // When inputs change, ensure element is appended and repositioned
    this.appendToBody();
    this.updatePositionStyles();
  }

  private appendToBody(): void {
    if (this.appended) return;
    try {
      // Append host element to body and make it fixed positioned so it floats above other content
      this.renderer.appendChild(document.body, this.host.nativeElement);
      this.renderer.setStyle(this.host.nativeElement, 'position', 'fixed');
      this.renderer.setStyle(this.host.nativeElement, 'z-index', '200000');
      this.appended = true;
      this.updatePositionStyles();
    } catch (e) {
      // ignore
    }
  }

  private updatePositionStyles(): void {
    if (!this.appended) return;
    try {
      if (this.overlayTop) this.renderer.setStyle(this.host.nativeElement, 'top', this.overlayTop);
      if (this.overlayLeft) this.renderer.setStyle(this.host.nativeElement, 'left', this.overlayLeft);
    } catch { }
  }

  ngOnDestroy(): void {
    if (!this.appended) return;
    try {
      if (this.host && this.host.nativeElement && this.host.nativeElement.parentNode === document.body) {
        this.renderer.removeChild(document.body, this.host.nativeElement);
      }
    } catch { }
    this.appended = false;
  }
}
