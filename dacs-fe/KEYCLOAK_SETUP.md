# Configuración de Keycloak para DACS Frontend

## Requisitos Previos

1. **Keycloak Server** ejecutándose en `http://localhost:8080`
2. **Realm** llamado `dacs` configurado
3. **Client** llamado `dacs-fe` configurado

## Configuración del Realm

### 1. Crear el Realm
1. Accede a la consola de administración de Keycloak: `http://localhost:8080`
2. Crea un nuevo realm llamado `dacs`

### 2. Configurar el Client
1. Ve a **Clients** → **Create**
2. **Client ID**: `dacs-fe`
3. **Client Protocol**: `openid-connect`
4. **Root URL**: `http://localhost:4200`
5. **Valid Redirect URIs**: `http://localhost:4200/*`
6. **Web Origins**: `*`
7. **Access Type**: `public`

### 3. Crear Roles
1. Ve a **Realm Roles** → **Create Role**
2. Crear los siguientes roles:
   - `ROLE-A` (para acceso a Tabla y Grilla)
   - `ROLE-B` (para acceso a Dashboard)

### 4. Crear Usuarios de Prueba

#### Usuario con ROLE-A
1. Ve a **Users** → **Add User**
2. **Username**: `user-a`
3. **Email**: `user-a@example.com`
4. **First Name**: `Usuario`
5. **Last Name**: `A`
6. **Email Verified**: ✅
7. **Enabled**: ✅
8. En la pestaña **Credentials**, establecer una contraseña local segura.
9. En la pestaña **Role Mapping**, asignar el rol `ROLE-A`

#### Usuario con ROLE-B
1. Ve a **Users** → **Add User**
2. **Username**: `user-b`
3. **Email**: `user-b@example.com`
4. **First Name**: `Usuario`
5. **Last Name**: `B`
6. **Email Verified**: ✅
7. **Enabled**: ✅
8. En la pestaña **Credentials**, establecer una contraseña local segura.
9. En la pestaña **Role Mapping**, asignar el rol `ROLE-B`

#### Usuario con ambos roles
1. Ve a **Users** → **Add User**
2. **Username**: `admin`
3. **Email**: `admin@example.com`
4. **First Name**: `Admin`
5. **Last Name**: `User`
6. **Email Verified**: ✅
7. **Enabled**: ✅
8. En la pestaña **Credentials**, establecer una contraseña local segura.
9. En la pestaña **Role Mapping**, asignar los roles `ROLE-A` y `ROLE-B`

## Configuración de la Aplicación

La configuración de Keycloak en la aplicación está en:
- `src/environments/environment.ts`
- `src/app/core/config/keycloak.config.ts`

### Variables de Entorno

```typescript
export const environment = {
  production: false,
  keycloak: {
    url: 'http://localhost:8080',
    realm: 'dacs',
    clientId: 'dacs-fe'
  },
  backendForFrontendUrl: 'http://localhost:9001/bff'
};
```

## Funcionalidades Implementadas

### 1. Autenticación
- Login automático con Keycloak
- Logout con redirección
- Verificación de sesión activa

### 2. Control de Acceso por Roles
- **ROLE-A**: Acceso a la pantalla de Tabla y Grilla
- **ROLE-B**: Acceso a la pantalla de Dashboard
- Guards de rutas que verifican roles antes de permitir acceso

### 3. Header de Usuario
- Información del usuario logueado
- Menú desplegable con perfil
- Botón de logout
- Enlace a la cuenta de usuario en Keycloak

### 4. Interfaz Adaptativa
- Botones deshabilitados si no se tiene el rol requerido
- Mensajes informativos sobre permisos requeridos
- Visualización de roles asignados al usuario

## Pruebas

### Escenario 1: Usuario no autenticado
1. Acceder a `http://localhost:4200`
2. Ver pantalla de login
3. Hacer clic en "Iniciar Sesión con Keycloak"
4. Ser redirigido a Keycloak para autenticarse

### Escenario 2: Usuario con ROLE-A
1. Login con `user-a` / la contraseña configurada en Keycloak
2. Ver botón "Tabla y Grilla" habilitado
3. Ver botón "Dashboard" deshabilitado
4. Acceder a `/table-grid` exitosamente
5. Intentar acceder a `/dashboard` → redirigido a `/unauthorized`

### Escenario 3: Usuario con ROLE-B
1. Login con `user-b` / la contraseña configurada en Keycloak
2. Ver botón "Dashboard" habilitado
3. Ver botón "Tabla y Grilla" deshabilitado
4. Acceder a `/dashboard` exitosamente
5. Intentar acceder a `/table-grid` → redirigido a `/unauthorized`

### Escenario 4: Usuario con ambos roles
1. Login con `admin` / la contraseña configurada en Keycloak
2. Ver ambos botones habilitados
3. Acceder a ambas pantallas exitosamente

## Solución de Problemas

### Error: "Keycloak not initialized"
- Verificar que Keycloak esté ejecutándose en `http://localhost:8080`
- Verificar la configuración del realm y client

### Error: "Invalid redirect URI"
- Verificar que `http://localhost:4200/*` esté en **Valid Redirect URIs**

### Error: "Access denied"
- Verificar que el usuario tenga los roles asignados correctamente
- Verificar la configuración de los guards en las rutas

### Error: "CORS"
- Verificar que `http://localhost:4200` esté en **Web Origins**

## Archivos Principales

- `src/app/core/services/keycloak.service.ts` - Servicio principal de Keycloak
- `src/app/core/guards/role.guard.ts` - Guards para control de acceso
- `src/app/header/` - Componente de header con perfil de usuario
- `src/app/home/` - Pantalla principal con control de acceso por roles
- `src/main.ts` - Inicialización de Keycloak
- `src/app/app.config.ts` - Configuración de la aplicación
