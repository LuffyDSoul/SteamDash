import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { KeycloakService } from '../services/keycloak.service';

/**
 * Guard para verificar roles específicos
 */
@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate {
  constructor(
    private keycloakService: KeycloakService,
    private router: Router
  ) {}

  canActivate(): Observable<boolean | UrlTree> {
    return new Observable(observer => {
      if (!this.keycloakService.isLoggedIn()) {
        observer.next(this.router.createUrlTree(['/login']));
        observer.complete();
        return;
      }

      // Verificar si tiene ROLE-A o ROLE-B
      const hasRequiredRole = this.keycloakService.hasAnyRole(['ROLE-A', 'ROLE-B']);
      
      if (hasRequiredRole) {
        observer.next(true);
      } else {
        observer.next(this.router.createUrlTree(['/unauthorized']));
      }
      
      observer.complete();
    });
  }
}

/**
 * Guard específico para ROLE-A
 */
@Injectable({
  providedIn: 'root'
})
export class RoleAGuard implements CanActivate {
  constructor(
    private keycloakService: KeycloakService,
    private router: Router
  ) {}

  canActivate(): Observable<boolean | UrlTree> {
    return new Observable(observer => {
      if (!this.keycloakService.isLoggedIn()) {
        observer.next(this.router.createUrlTree(['/login']));
        observer.complete();
        return;
      }

      if (this.keycloakService.hasRole('ROLE-A')) {
        observer.next(true);
      } else {
        observer.next(this.router.createUrlTree(['/unauthorized']));
      }
      
      observer.complete();
    });
  }
}

/**
 * Guard específico para ROLE-B
 */
@Injectable({
  providedIn: 'root'
})
export class RoleBGuard implements CanActivate {
  constructor(
    private keycloakService: KeycloakService,
    private router: Router
  ) {}

  canActivate(): Observable<boolean | UrlTree> {
    return new Observable(observer => {
      if (!this.keycloakService.isLoggedIn()) {
        observer.next(this.router.createUrlTree(['/login']));
        observer.complete();
        return;
      }

      if (this.keycloakService.hasRole('ROLE-B')) {
        observer.next(true);
      } else {
        observer.next(this.router.createUrlTree(['/unauthorized']));
      }
      
      observer.complete();
    });
  }
}
