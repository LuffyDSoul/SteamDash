import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLinkActive, RouterModule } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { KeycloakService } from '../../core/services/keycloak.service';
import { KeycloakProfile } from 'keycloak-js';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterLinkActive],
  templateUrl: './header.html',
  styleUrls: ['./header.css']
})
export class HeaderComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  isLoggedIn = false;
  userProfile: KeycloakProfile | null = null;
  showUserMenu = false;

  constructor(private keycloakService: KeycloakService) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const userProfile = target.closest('.user-profile');
    const mobileMenu = target.closest('.nav-mobile');
    const burger = target.closest('.burger');
    const navToggle = target.closest('.nav-toggle');
    
    // Cerrar menú de usuario si se hace click fuera
    if (!userProfile && this.showUserMenu) {
      this.showUserMenu = false;
    }
    
    // Cerrar menú móvil si se hace click fuera
    if (!mobileMenu && !burger && !navToggle) {
      const navToggleEl = document.getElementById('nav-toggle') as HTMLInputElement;
      if (navToggleEl && navToggleEl.checked) {
        // Solo cerrar si el click no fue en un link del menú
        if (!target.closest('a[routerLink]')) {
          navToggleEl.checked = false;
        }
      }
    }
  }

  ngOnInit(): void {
    this.checkLoginStatus();
    this.subscribeToUserProfile();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private checkLoginStatus(): void {
    this.isLoggedIn = this.keycloakService.isLoggedIn();
    if (this.isLoggedIn) {
      this.userProfile = this.keycloakService.getUserProfile();
    }
  }

  private subscribeToUserProfile(): void {
    this.keycloakService.userProfile$
      .pipe(takeUntil(this.destroy$))
      .subscribe(profile => {
        this.userProfile = profile;
        this.isLoggedIn = !!profile;
      });
  }

  login(): void {
    this.keycloakService.login();
  }

  logout(): void {
    this.showUserMenu = false;
    this.keycloakService.logout();
  }

  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  closeUserMenu(): void {
    this.showUserMenu = false;
  }

  getFullName(): string {
    return this.keycloakService.getFullName();
  }

  getEmail(): string {
    return this.keycloakService.getEmail();
  }

  getUsername(): string {
    return this.keycloakService.getUsername();
  }

  getUserRoles(): string[] {
    return this.keycloakService.getUserRoles();
  }

  hasRole(role: string): boolean {
    return this.keycloakService.hasRole(role);
  }

  goToAccount(): void {
    this.showUserMenu = false;
    window.open(this.keycloakService.getAccountUrl(), '_blank');
  }
  
  closeMenu() {
    const el = document.getElementById('nav-toggle') as HTMLInputElement | null;
    if (el) el.checked = false;
  }
}
