import { Injectable } from '@angular/core';
import { KeycloakService as KeycloakAngularService } from 'keycloak-angular';
import { KeycloakProfile } from 'keycloak-js';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Servicio personalizado para manejar Keycloak
 */
@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  private userProfileSubject = new BehaviorSubject<KeycloakProfile | null>(null);
  public userProfile$ = this.userProfileSubject.asObservable();

  constructor(private keycloak: KeycloakAngularService) {
    this.initializeUserProfile();
  }

  /**
   * Inicializa el perfil del usuario
   */
  private async initializeUserProfile(): Promise<void> {
    if (this.isLoggedIn()) {
      try {
        const profile = await this.keycloak.loadUserProfile();
        this.userProfileSubject.next(profile);
      } catch (error) {
        console.error('Error cargando perfil de usuario:', error);
      }
    }
  }

  /**
   * Verifica si el usuario está logueado
   */
  isLoggedIn(): boolean {
    return this.keycloak.isLoggedIn();
  }

  /**
   * Obtiene el token de acceso
   */
  getToken(): Promise<string> {
    return this.keycloak.getToken();
  }

  /**
   * Obtiene el perfil del usuario actual
   */
  getUserProfile(): KeycloakProfile | null {
    return this.userProfileSubject.value;
  }

  /**
   * Obtiene los roles del usuario
   */
  getUserRoles(): string[] {
    return this.keycloak.getUserRoles();
  }

  /**
   * Verifica si el usuario tiene un rol específico
   */
  hasRole(role: string): boolean {
    return this.keycloak.isUserInRole(role);
  }

  /**
   * Verifica si el usuario tiene alguno de los roles especificados
   */
  hasAnyRole(roles: string[]): boolean {
    return roles.some(role => this.hasRole(role));
  }

  /**
   * Verifica si el usuario tiene todos los roles especificados
   */
  hasAllRoles(roles: string[]): boolean {
    return roles.every(role => this.hasRole(role));
  }

  /**
   * Inicia el proceso de login
   */
  login(): Promise<void> {
    return this.keycloak.login();
  }

  /**
   * Cierra la sesión
   */
  logout(): Promise<void> {
    return this.keycloak.logout();
  }

  /**
   * Obtiene la URL de la cuenta de usuario
   */
  getAccountUrl(): string {
    return this.keycloak.getKeycloakInstance().createAccountUrl();
  }

  /**
   * Obtiene el nombre completo del usuario
   */
  getFullName(): string {
    const profile = this.getUserProfile();
    if (profile?.firstName && profile?.lastName) {
      return `${profile.firstName} ${profile.lastName}`;
    }
    return profile?.username || 'Usuario';
  }

  /**
   * Obtiene el email del usuario
   */
  getEmail(): string {
    const profile = this.getUserProfile();
    return profile?.email || '';
  }

  /**
   * Obtiene el username del usuario
   */
  getUsername(): string {
    const profile = this.getUserProfile();
    return profile?.username || '';
  }

  /**
   * Refresca el perfil del usuario
   */
  async refreshUserProfile(): Promise<void> {
    if (this.isLoggedIn()) {
      try {
        const profile = await this.keycloak.loadUserProfile();
        this.userProfileSubject.next(profile);
      } catch (error) {
        console.error('Error refrescando perfil de usuario:', error);
      }
    }
  }
}
