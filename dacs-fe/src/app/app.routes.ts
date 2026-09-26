import { Routes } from '@angular/router';
import { RoleAGuard } from './core/guards/role.guard';
import { RoleBGuard } from './core/guards/role.guard';
import { CompareComponent } from './features/compare/compare.component';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'home', loadComponent: () => import('./components/home/home').then(m => m.HomeComponent) },
  { 
    path: 'table-grid', 
    loadComponent: () => import('./table-grid/table-grid').then(m => m.TableGridComponent),
    canActivate: [RoleAGuard]
  },
  { 
   path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard-home/dashboard-home').then(m => m.DashboardHome),
    canActivate: [RoleBGuard]
  },
  {path:'news', 
    loadComponent: () => import('./features/news-home/news-home.component').then(m => m.NewsHomeComponent),
    canActivate: [RoleBGuard]  
  },
  { path: 'compare', component: CompareComponent },  

  // Biblioteca de usuario individual
  { path: 'biblioteca', loadComponent: () => import('./features/biblioteca-usuario/biblioteca-usuario.component').then(m => m.BibliotecaUsuarioComponent) },
  { path: 'biblioteca/:steamId', loadComponent: () => import('./features/biblioteca-usuario/biblioteca-usuario.component').then(m => m.BibliotecaUsuarioComponent) },
  
  // Juego aleatorio de biblioteca
  { path: 'biblioteca/:steamId/aleatorio', loadComponent: () => import('./features/juego-aleatorio/juego-aleatorio.component').then(m => m.JuegoAleatorioComponent) },

  // Páginas legales
  { path: 'privacidad', loadComponent: () => import('./components/legal/privacidad.component').then(m => m.PrivacidadComponent) },
  { path: 'terminos', loadComponent: () => import('./components/legal/terminos.component').then(m => m.TerminosComponent) },
  { path: 'nosotros', loadComponent: () => import('./components/legal/nosotros.component').then(m => m.NosotrosComponent) },

  // Estadísticas de biblioteca
  { path: 'estadisticas', loadComponent: () => import('./features/estadisticas/estadisticas.component').then(m => m.EstadisticasComponent) },
  { path: 'estadisticas/:steamId', loadComponent: () => import('./features/estadisticas/estadisticas.component').then(m => m.EstadisticasComponent) },

  // default and fallback
  { path: '', pathMatch: 'full', redirectTo: '/home' },
  { path: '**', redirectTo: '/home' }
];
