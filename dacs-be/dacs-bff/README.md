# DACS Steam API - Backend For Frontend (BFF)

Microservicio BFF que actúa como punto de entrada único para el frontend, orquestando las comunicaciones con los microservicios de backend y conector Steam.

## Arquitectura

```
Cliente/Frontend
       ↓
   DACS-BFF (9001)
       ↓                    ↓
DACS-Backend (9003)  DACS-Conector (9002)
                           ↓
                     Steam API Externa
```

## Ejecución Local

```bash
mvn clean spring-boot:run
```

Opcional con perfil local:
```bash
mvn clean spring-boot:run -P local
```

El servicio estará disponible en: **http://localhost:9001**

## Endpoints Disponibles

### Control y Salud
- `GET /` - Saludo del servicio BFF
- `GET /ping` - Health check del BFF
- `GET /status` - Status agregado de todos los servicios
- `GET /backendping` - Health check del backend via BFF
- `GET /conectorping` - Health check del conector via BFF
- `GET /version` - Información de build

### Steam Applications
- `GET /apps` - Listar aplicaciones (con filtros opcionales)
- `GET /apps/{id}` - Obtener aplicación por ID interno
- `GET /apps/steam/{steamAppId}` - Obtener aplicación por Steam App ID
- `POST /apps` - Crear nueva aplicación
- `PUT /apps/{id}` - Actualizar aplicación
- `DELETE /apps/{id}` - Eliminar aplicación
- `GET /apps/gratuitas` - Listar aplicaciones gratuitas

#### Filtros para /apps:
- `titulo` - Filtrar por título
- `desarrolladora` - Filtrar por desarrolladora
- `publisher` - Filtrar por publisher
- `gratuito` - Solo aplicaciones gratuitas (true/false)
- `generos` - Filtrar por géneros (separados por coma)
- `categorias` - Filtrar por categorías (separados por coma)

### Usuarios Steam
- `GET /usuarios` - Listar todos los usuarios
- `GET /usuarios/{id}` - Obtener usuario por ID
- `GET /usuarios/steam/{steamId}` - Obtener usuario por Steam ID
- `POST /usuarios` - Crear nuevo usuario
- `PUT /usuarios/{id}` - Actualizar usuario
- `DELETE /usuarios/{id}` - Eliminar usuario

### Logros y Progreso
- `GET /logros` - Listar todos los logros obtenidos
- `GET /logros/usuario/{idUsuario}` - Logros de un usuario
- `GET /logros/aplicacion/{idAplicacion}` - Logros de una aplicación
- `GET /logros/usuario/{idUsuario}/aplicacion/{idAplicacion}` - Logros específicos usuario-app
- `POST /logros/usuario/{idUsuario}/logro/{idLogro}/marcar` - Marcar logro como obtenido
- `GET /logros/usuario/{idUsuario}/aplicacion/{idAplicacion}/progreso` - Calcular progreso

### Steam API Externa
- `GET /steam/game/{appId}` - Obtener detalles de juego desde Steam API

## Configuración

### Propiedades requeridas

```properties
# URLs de los microservicios
feign.client.config.apiBackendClient.url=http://localhost:9003
feign.client.config.apiconectorclient.url=http://localhost:9002
```

## Estructura del Proyecto

```
src/main/java/com/dacs/bff/
├── controller/          # Controladores REST del BFF
│   ├── HomeController.java
│   ├── SteamAplicacionController.java
│   ├── UsuarioController.java
│   ├── LogroObtenidoController.java
│   ├── SteamController.java
├── service/             # Servicios de orquestación
│   ├── ApiBackendService.java
│   ├── ApiBackendServiceImpl.java
│   ├── ApiConectorService.java
│   └── ApiConectorServiceImpl.java
├── api/client/          # Clientes Feign para microservicios
│   ├── ApiBackendClient.java
│   └── ApiConectorClient.java
├── dto/                 # DTOs para transferencia de datos
│   ├── AplicacionDto.java
│   ├── UsuarioDto.java
│   ├── LogroObtenidoDto.java
│   ├── SteamGameDto.java
│   ├── AlumnoDto.java (legacy)
│   └── ItemDto.java (legacy)
└── exception/           # Manejo de excepciones
```

## Funcionalidades del BFF

1. **Punto de entrada único**: Simplifica la comunicación frontend-backend
2. **Orquestación de servicios**: Coordina llamadas entre backend y conector
3. **Agregación de datos**: Combina respuestas de múltiples servicios
4. **Transformación de datos**: Adapta DTOs para necesidades del frontend
5. **Manejo de errores centralizado**: Control unificado de excepciones
6. **Logging estructurado**: Trazabilidad de operaciones

## Health Checks

- **BFF Health**: http://localhost:9001/ping
- **Status agregado**: http://localhost:9001/status
- **Métricas**: http://localhost:9001/metrics/health

## Desarrollo

Para desarrollo con recarga automática:
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"
```
