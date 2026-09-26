import { Component, OnInit, OnDestroy, inject, AfterViewInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { KeycloakService } from '../../core/services/keycloak.service';
import { SteamApiService } from '../../core/models/steam/steam-api-response';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLink],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit, OnDestroy, AfterViewInit {
  private destroy$ = new Subject<void>();
  
  title = 'home';
  isLoggedIn = false;
  hasRoleA = false;
  hasRoleB = false;

  constructor(public keycloakService: KeycloakService) {}

  ngOnInit(): void {
    this.checkLoginStatus();
    this.subscribeToUserProfile();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngAfterViewInit(): void {
    // Iniciar observador de scroll para animaciones
    this.initScrollReveal();
  }

  private checkLoginStatus(): void {
    this.isLoggedIn = this.keycloakService.isLoggedIn();
    this.updateRoleStatus();
  }

  private subscribeToUserProfile(): void {
    this.keycloakService.userProfile$
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.isLoggedIn = this.keycloakService.isLoggedIn();
        this.updateRoleStatus();
      });
  }

  private updateRoleStatus(): void {
    this.hasRoleA = this.keycloakService.hasRole('ROLE-A');
    this.hasRoleB = this.keycloakService.hasRole('ROLE-B');
  }

  login(): void {
    this.keycloakService.login();
  }

  canAccessTableGrid(): boolean {
    return this.isLoggedIn && this.hasRoleA;
  }

  canAccessDashboard(): boolean {
    return this.isLoggedIn && this.hasRoleB;
  }

  getAccessMessage(role: string): string {
    if (!this.isLoggedIn) {
      return 'Inicia sesión para acceder';
    }
    return `Se requiere ${role} para acceder`;
  }

  private api = inject(SteamApiService);

  recent$ = this.api.games$.pipe(
    map(gs => gs
      .filter(g => (g.lastTwoWeeks ?? 0) > 0)
      .sort((a,b) => (b.lastTwoWeeks ?? 0) - (a.lastTwoWeeks ?? 0))
      .slice(0, 5))
  );

  // Scroll Reveal con Intersection Observer
  private initScrollReveal(): void {
    const observerOptions = {
      threshold: 0.1,
      rootMargin: '0px 0px -100px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('revealed');
          observer.unobserve(entry.target);
        }
      });
    }, observerOptions);

    // Observar elementos con clases reveal
    const revealElements = document.querySelectorAll('.reveal-left, .reveal-right, .reveal-fade');
    revealElements.forEach(el => observer.observe(el));
  }

  // Efecto parallax en scroll
  @HostListener('window:scroll')
  onScroll(): void {
    const scrolled = window.pageYOffset;
    
    // Parallax en hero background
    const heroBg = document.querySelector('.hero-bg') as HTMLElement;
    if (heroBg) {
      heroBg.style.transform = `translateY(${scrolled * 0.5}px)`;
    }

    // Parallax en elementos flotantes
    const floatElements = document.querySelectorAll('.float-element') as NodeListOf<HTMLElement>;
    floatElements.forEach((el, index) => {
      const speed = 0.3 + (index * 0.1);
      el.style.transform = `translate(0, ${scrolled * speed}px)`;
    });
  }
}
