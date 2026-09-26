# Integración del BFF en Angular (guía rápida)

Pasos para integrar los endpoints del `dacs-bff` en la aplicación Angular del frontend.

## 1) Proxy para desarrollo
Crea `proxy.conf.json` en la raíz del frontend (ya incluido en el repo):

```json
{
  "/bff": {
    "target": "http://localhost:9001",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

Y arranca Angular con:

```bash
ng serve --proxy-config proxy.conf.json
```

Esto evita problemas de CORS en desarrollo y mantiene las rutas del BFF bajo `/bff`.

## 2) Servicio Angular
Se creó `src/app/services/bff.service.ts` con métodos:
- `getAllUsuarios()` => GET `/bff/usuarios`
- `getUsuarioById(id)` => GET `/bff/usuarios/{id}`
- `compareLibraries(steamId1, steamId2)` => GET `/bff/usuarios/comparar/{steamId1}/con/{steamId2}`

## 3) Modelos TypeScript
Se agregaron interfaces en `src/app/models/` para tipado del resultado.

## 4) Interceptor de Auth
Se añadió un `AuthInterceptor` básico en `src/app/interceptors/auth.interceptor.ts` que añade el header `Authorization: Bearer <token>` si existe en `sessionStorage`.

Registra el interceptor en `app.module.ts`:

```ts
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from './interceptors/auth.interceptor';

providers: [
  { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
]
```

## 5) Componente de ejemplo
Se agregó `compare-libraries` como ejemplo (código en `src/app/components/compare-libraries/`), muestra estado y resultados.

## 6) Tests
Se agregó spec para el componente con `HttpClient` simulado.

## 7) Endpoints a usar
- `GET /bff/usuarios` - Listar usuarios
- `GET /bff/usuarios/{id}` - Obtener usuario
- `GET /bff/usuarios/comparar/{steamId1}/con/{steamId2}` - Comparar bibliotecas

## 8) Notas de despliegue
- En producción, configura la URL base del BFF en `environment.ts`.
- Si usas Keycloak, integra el token en `AuthInterceptor` desde tu servicio de autenticación.

## 9) Próximos pasos sugeridos
- Mejorar UI/UX del componente con paginación y filtros
- Implementar cache en el BFF o en frontend para reducir llamadas repetidas
- Manejar usuarios sin biblioteca o con bibliotecas muy grandes (paginación)

---

Si quieres, puedo:
- Añadir la integración de Keycloak (ejemplo con `keycloak-angular`)
- Crear un módulo Angular `bff` con todos los servicios y componentes listos
- Escribir tests adicionales para el servicio `BffService`

Dime qué prefieres que haga a continuación.